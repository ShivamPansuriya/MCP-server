package com.example.mcpserver.tools.builtin;

import com.example.mcpserver.service.IncidentStorageService;
import com.example.mcpserver.tool.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Tool for performing partial updates (PATCH operations) on existing incidents.
 * Accepts incident ID and a dynamic updates object containing only the fields to be modified.
 * Use get_fields_schema first to understand available fields and their constraints.
 */
@Component
public class UpdateIncidentTool implements McpTool {

    private static final Logger logger = LoggerFactory.getLogger(UpdateIncidentTool.class);

    private final IncidentStorageService incidentStorageService;
    private final ObjectMapper objectMapper;
    
    private static final String SCHEMA = """
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "description": "Perform partial update (PATCH) operation on an existing incident. Only include fields that need to be changed.",
                "properties": {
                    "incident_id": {
                        "type": "string",
                        "description": "Unique identifier of the incident to update. Must be in format INC-XXXXXXXX where X is a hexadecimal digit (0-9, A-F).",
                        "pattern": "^INC-[A-F0-9]{8}$",
                        "examples": ["INC-1234ABCD", "INC-9876EF01", "INC-DEADBEEF"]
                    },
                    "field_updates": {
                        "type": "object",
                        "description": "Object containing only the fields to be updated and their new values. Do not include fields that should remain unchanged. Use get_fields_schema tool first to see available fields and their validation rules. Object containing incident field data with field names as keys and field values as values. Use exact field names from get_fields_schema tool response.",
                        "additionalProperties": false,
                        "minProperties": 1,
                        "examples": [
                            {"priority": "High", "status": "In Progress"},
                            {"title": "Updated incident title", "description": "Updated description with more details"},
                            {"status": "Resolved"}
                        ]
                    }
                },
                "required": ["incident_id", "field_updates"],
                "additionalProperties": false,
                "examples": [
                    {
                        "incident_id": "INC-1234ABCD",
                        "field_updates": {"priority": "High", "status": "In Progress"}
                    },
                    {
                        "incident_id": "INC-9876EF01",
                        "field_updates": {"title": "Database connectivity issues", "description": "Multiple users reporting connection timeouts"}
                    }
                ]
            }
            """;

    @Autowired
    public UpdateIncidentTool(IncidentStorageService incidentStorageService, ObjectMapper objectMapper) {
        this.incidentStorageService = incidentStorageService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "update_incident";
    }
    
    @Override
    public String getDescription() {
        return "Update fields of an existing incident. First call get_updatable_fields to understand available fields and their constraints, then use this tool with the incident name and update data.";
    }
    
    @Override
    public String getSchema() {
        return SCHEMA;
    }
    
    @Override
    public ToolMetadata getMetadata() {
        return ToolMetadata.builder()
                .name(getName())
                .description(getDescription())
                .version("1.0.0")
                .category("incident-management")
                .schema(getSchema())
                .build();
    }
    
    @Override
    public ValidationResult validate(Map<String, Object> arguments) {
        logger.debug("Validating update_incident arguments: {}", arguments.keySet());

        // Validate incident_id parameter
        if (!arguments.containsKey("incident_id") || arguments.get("incident_id") == null) {
            logger.warn("Validation failed: missing 'incident_id' parameter");
            return ValidationResult.failure("'incident_id' parameter is required");
        }

        String incidentId = arguments.get("incident_id").toString().trim();
        if (incidentId.isEmpty()) {
            logger.warn("Validation failed: empty 'incident_id' parameter");
            return ValidationResult.failure("'incident_id' cannot be empty");
        }

        // Validate incident ID format (INC-XXXXXXXX where X is hex digit)
        if (!incidentId.matches("^INC-[A-F0-9]{8}$")) {
            logger.warn(
                    "Validation failed: invalid incident ID format '{}'. Expected format: INC-XXXXXXXX (hex digits)",
                    incidentId);
            return ValidationResult.failure(
                    "'incident_id' must be in format INC-XXXXXXXX where X is a hexadecimal digit (0-9, A-F)");
        }

        // Validate field_updates parameter
        if (!arguments.containsKey("field_updates") || arguments.get("field_updates") == null) {
            logger.warn("Validation failed: missing 'field_updates' parameter");
            return ValidationResult.failure("'field_updates' parameter is required");
        }
        return ValidationResult.success();
    }

    @Override
    public Mono<ToolResult> execute(McpAsyncServerExchange exchange, ToolContext context,
            Map<String, Object> arguments) {
        logger.info("Executing update_incident tool for session: {}", context.getSessionId());

        return Mono.fromCallable(() -> {
            String incidentId = arguments.get("incident_id").toString().trim();

            @SuppressWarnings("unchecked") Map<String, Object> fieldUpdates =
                    (Map<String, Object>) arguments.get("field_updates");

            logger.debug("Attempting to update incident '{}' with {} fields: {}", incidentId,
                    fieldUpdates.size(), fieldUpdates.keySet());

            // Check if incident exists
            if (!incidentStorageService.incidentExists(incidentId)) {
                logger.warn("Update failed: incident '{}' not found", incidentId);
                Map<String, Object> errorResponse =
                        Map.of("success", false, "error_code", "INCIDENT_NOT_FOUND", "message",
                                "Incident with ID '" + incidentId + "' was not found", "incident_id",
                                incidentId, "updated_incident", Map.of());
                String responseJson = objectMapper.writeValueAsString(errorResponse);
                return ToolResult.success(List.of(new McpSchema.TextContent(responseJson)));
            }

            // Perform the patch update (IncidentStorageService handles detailed validation)
            Map<String, Object> updatedIncident =
                    incidentStorageService.updateIncident(incidentId, fieldUpdates);

            logger.info("Successfully updated incident '{}' with {} fields: {}", incidentId,
                    fieldUpdates.size(), fieldUpdates.keySet());
            logger.debug("Updated incident data: {}", updatedIncident);

            // Format success response
            Map<String, Object> response = Map.of("success", true, "message",
                    "Incident updated successfully with " + fieldUpdates.size() + " field(s)", "incident_id",
                    incidentId, "updated_fields", fieldUpdates.keySet(), "updated_incident", updatedIncident);
            String responseJson = objectMapper.writeValueAsString(response);

            return ToolResult.success(List.of(new McpSchema.TextContent(responseJson)));
        }).doOnError(error -> logger.error("Failed to update incident '{}': {}", arguments.get("incident_id"),
                error.getMessage(), error)).onErrorResume(error -> {
            // Provide structured error response
            try {
                Map<String, Object> errorResponse =
                        Map.of("success", false, "error_code", "UPDATE_FAILED", "message",
                                "Failed to update incident: " + error.getMessage(), "incident_id",
                                arguments.get("incident_id"), "updated_incident", Map.of());
                String errorJson = objectMapper.writeValueAsString(errorResponse);
                return Mono.just(ToolResult.success(List.of(new McpSchema.TextContent(errorJson))));
            } catch (Exception e) {
                return Mono.just(ToolResult.error("Failed to update incident: " + error.getMessage()));
            }
        });
    }
}