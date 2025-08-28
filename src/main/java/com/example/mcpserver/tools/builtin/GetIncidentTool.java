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
 * Tool for retrieving incident details by ID.
 * Fetches complete incident information including all fields and metadata.
 */
@Component
public class GetIncidentTool implements McpTool {

    private static final Logger logger = LoggerFactory.getLogger(GetIncidentTool.class);

    private final IncidentStorageService incidentStorageService;
    private final ObjectMapper objectMapper;
    
    private static final String SCHEMA = """
        {
            "$schema": "http://json-schema.org/draft-07/schema#",
            "type": "object",
            "description": "Retrieve incident details by incident ID. Returns complete incident information including all fields and metadata.",
            "properties": {
                "incident_id": {
                    "type": "string",
                    "description": "The unique incident ID to retrieve (format: INC-XXXXXXXX)",
                    "pattern": "^INC-[A-F0-9]{8}$"
                }
            },
            "required": ["incident_id"],
            "additionalProperties": false,
            "examples": [
                {
                    "incident_id": "INC-1234ABCD"
                },
                {
                    "incident_id": "INC-9876EF01"
                }
            ]
        }
        """;
    
    @Autowired
    public GetIncidentTool(IncidentStorageService incidentStorageService, ObjectMapper objectMapper) {
        this.incidentStorageService = incidentStorageService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "get_incident";
    }
    
    @Override
    public String getDescription() {
        return "Retrieve incident details by incident ID. Returns complete incident information including all fields, status, priority, timestamps, and other metadata.";
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
                .category("request-management")
                .schema(getSchema())
                .build();
    }
    
    @Override
    public ValidationResult validate(Map<String, Object> arguments) {
        logger.debug("Validating get_incident arguments: {}", arguments.keySet());

        if (arguments == null || arguments.isEmpty()) {
            logger.warn("Validation failed: no arguments provided");
            return ValidationResult.failure("Arguments are required. Please provide 'incident_id' parameter.");
        }

        // Check for incident_id parameter
        if (!arguments.containsKey("incident_id") || arguments.get("incident_id") == null) {
            logger.warn("Validation failed: missing 'incident_id' parameter");
            return ValidationResult.failure("'incident_id' parameter is required.");
        }

        Object incidentIdObj = arguments.get("incident_id");
        if (!(incidentIdObj instanceof String)) {
            logger.warn("Validation failed: 'incident_id' must be a string");
            return ValidationResult.failure("'incident_id' must be a string.");
        }

        String incidentId = (String) incidentIdObj;
        if (incidentId.trim().isEmpty()) {
            logger.warn("Validation failed: 'incident_id' cannot be empty");
            return ValidationResult.failure("'incident_id' cannot be empty.");
        }

        // Validate incident ID format (INC-XXXXXXXX)
        if (!incidentId.matches("^INC-[A-F0-9]{8}$")) {
            logger.warn("Validation failed: invalid incident ID format: {}", incidentId);
            return ValidationResult.failure("Invalid incident ID format. Expected format: INC-XXXXXXXX (e.g., INC-1234ABCD)");
        }

        logger.debug("Validation successful for get_incident with incident_id: {}", incidentId);
        return ValidationResult.success();
    }

    @Override
    public Mono<ToolResult> execute(McpAsyncServerExchange exchange, ToolContext context, Map<String, Object> arguments) {
        logger.info("Executing get_incident tool for session: {}", context.getSessionId());

        return Mono.fromCallable(() -> {
            // Extract incident_id from arguments
            String incidentId = (String) arguments.get("incident_id");
            
            logger.debug("Retrieving incident with ID: {}", incidentId);

            // Retrieve incident from storage
            Map<String, Object> incident = incidentStorageService.getIncident(incidentId);
            
            if (incident == null) {
                logger.warn("Incident not found: {}", incidentId);
                
                // Return structured error response
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error_code", "INCIDENT_NOT_FOUND",
                    "message", "Incident with ID '" + incidentId + "' was not found",
                    "incident_id", incidentId,
                    "incident", Map.of()
                );
                
                String responseJson = objectMapper.writeValueAsString(errorResponse);
                return ToolResult.success(List.of(new McpSchema.TextContent(responseJson)));
            }

            logger.info("Successfully retrieved incident: {}", incidentId);
            logger.debug("Incident details: {}", incident);

            // Format successful response
            Map<String, Object> successResponse = Map.of(
                "success", true,
                "message", "Incident retrieved successfully",
                "incident_id", incidentId,
                "incident", incident
            );

            String responseJson = objectMapper.writeValueAsString(successResponse);
            
            logger.debug("Returning successful response for incident: {}", incidentId);
            return ToolResult.success(List.of(new McpSchema.TextContent(responseJson)));
        })
        .doOnError(error -> logger.error("Failed to retrieve incident: {}", error.getMessage(), error))
        .onErrorResume(error -> Mono.just(ToolResult.error("Failed to retrieve incident: " + error.getMessage())));
    }
}
