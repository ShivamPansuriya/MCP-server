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
 * Tool for updating existing incident fields with simplified input schema.
 * Accepts only incident name and update data. Field structure and constraints
 * should be obtained from get_updatable_fields tool first. Internal validation
 * is handled by IncidentStorageService.
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
            "properties": {
                "name": {
                    "type": "string",
                    "description": "ID of the incident to update (e.g., INC-1234ABCD)."
                },
                "updates": {
                    "type": "object",
                    "description": "Object containing the fields to update and their new values. Use get_updatable_fields to understand the structure and constraints of available fields."
                }
            },
            "required": ["name", "updates"]
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

        // Check required fields
        if (!arguments.containsKey("name") || arguments.get("name") == null) {
            logger.warn("Validation failed: missing 'name' parameter");
            return ValidationResult.failure("'name' parameter is required");
        }

        if (!arguments.containsKey("updates") || arguments.get("updates") == null) {
            logger.warn("Validation failed: missing 'updates' parameter");
            return ValidationResult.failure("'updates' parameter is required");
        }

        String incidentId = arguments.get("name").toString().trim();
        if (incidentId.isEmpty()) {
            logger.warn("Validation failed: empty 'name' parameter");
            return ValidationResult.failure("'name' cannot be empty");
        }

        // Validate incident ID format
        if (!incidentId.matches("^INC-[A-F0-9]{8}$")) {
            logger.warn("Validation failed: invalid incident name format '{}'. Expected format: INC-XXXXXXXX", incidentId);
            return ValidationResult.failure("'name' must be in format INC-XXXXXXXX where X is a hex digit");
        }

        // Basic validation that updates is provided (detailed validation handled by IncidentStorageService)
        Object updatesObj = arguments.get("updates");
        if (updatesObj == null) {
            logger.warn("Validation failed: 'updates' parameter is null");
            return ValidationResult.failure("'updates' cannot be null");
        }

        logger.debug("Basic validation successful for update_incident with incident name: '{}'", incidentId);
        return ValidationResult.success();
    }
    
    @Override
    public Mono<ToolResult> execute(McpAsyncServerExchange exchange, ToolContext context, Map<String, Object> arguments) {
        logger.info("Executing update_incident tool for session: {}", context.getSessionId());

        return Mono.fromCallable(() -> {
            String incidentId = arguments.get("name").toString().trim();

            @SuppressWarnings("unchecked")
            Map<String, Object> updates = (Map<String, Object>) arguments.get("updates");

            logger.debug("Attempting to update incident '{}' with fields: {}", incidentId, updates != null ? updates.keySet() : "null");

            // Check if incident exists
            if (!incidentStorageService.incidentExists(incidentId)) {
                logger.warn("Update failed: incident '{}' not found", incidentId);
                Map<String, Object> errorResponse = Map.of(
                    "message", "Incident not found",
                    "updated_incident", Map.of()
                );
                String responseJson = objectMapper.writeValueAsString(errorResponse);
                return ToolResult.success(List.of(new McpSchema.TextContent(responseJson)));
            }

            // Update the incident (IncidentStorageService will handle validation)
            Map<String, Object> updatedIncident = incidentStorageService.updateIncident(incidentId, updates);

            logger.info("Successfully updated incident '{}' with {} fields", incidentId, updates != null ? updates.size() : 0);
            logger.debug("Updated incident data: {}", updatedIncident);

            // Format response
            Map<String, Object> response = Map.of(
                "message", "Incident updated successfully",
                "updated_incident", updatedIncident
            );
            String responseJson = objectMapper.writeValueAsString(response);

            return ToolResult.success(
                List.of(new McpSchema.TextContent(responseJson))
            );
        })
        .doOnError(error -> logger.error("Failed to update incident '{}': {}", arguments.get("name"), error.getMessage(), error))
        .onErrorResume(error -> Mono.just(ToolResult.error("Failed to update incident: " + error.getMessage())));
    }
}
