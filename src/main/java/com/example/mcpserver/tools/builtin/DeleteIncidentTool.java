package com.example.mcpserver.tools.builtin;

import com.example.mcpserver.service.IncidentStorageService;
import com.example.mcpserver.tool.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tool for safely deleting incidents with user confirmation.
 * Implements a two-step process: first shows incident details and requests confirmation,
 * then performs the actual deletion after user confirms.
 */
@Component
public class DeleteIncidentTool implements McpTool {

    private static final Logger logger = LoggerFactory.getLogger(DeleteIncidentTool.class);

    private final IncidentStorageService incidentStorageService;
    private final ObjectMapper objectMapper;
    
    private static final String SCHEMA = """
        {
            "$schema": "http://json-schema.org/draft-07/schema#",
            "type": "object",
            "properties": {
                "incident_id": {
                    "type": "string",
                    "description": "ID of the incident to delete (e.g., INC-1234ABCD).",
                    "pattern": "^INC-[A-F0-9]{8}$"
                },
                "confirm_deletion": {
                    "type": "boolean",
                    "description": "Set to true to confirm deletion after reviewing incident details. Required for actual deletion.",
                    "default": false
                }
            },
            "required": ["incident_id"]
        }
        """;

    @Autowired
    public DeleteIncidentTool(IncidentStorageService incidentStorageService, ObjectMapper objectMapper) {
        this.incidentStorageService = incidentStorageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "delete_incident";
    }

    @Override
    public String getDescription() {
        return "Safely delete an incident with user confirmation. First call shows incident details, second call with confirm_deletion=true performs deletion.";
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
        logger.debug("Validating delete_incident arguments: {}", arguments);

        // Validate incident_id parameter
        if (!arguments.containsKey("incident_id")) {
            logger.warn("Validation failed: missing required parameter 'incident_id'");
            return ValidationResult.failure("Missing required parameter: 'incident_id'");
        }

        Object incidentIdObj = arguments.get("incident_id");
        if (incidentIdObj == null) {
            logger.warn("Validation failed: 'incident_id' cannot be null");
            return ValidationResult.failure("'incident_id' cannot be null");
        }
        
        String incidentId = incidentIdObj.toString().trim();
        if (incidentId.isEmpty()) {
            logger.warn("Validation failed: empty 'incident_id' parameter");
            return ValidationResult.failure("'incident_id' cannot be empty");
        }

        // Validate incident ID format
        if (!incidentId.matches("^INC-[A-F0-9]{8}$")) {
            logger.warn("Validation failed: invalid incident_id format '{}'. Expected format: INC-XXXXXXXX", incidentId);
            return ValidationResult.failure("'incident_id' must be in format INC-XXXXXXXX where X is a hex digit");
        }

        // Validate confirm_deletion parameter if present
        if (arguments.containsKey("confirm_deletion")) {
            Object confirmObj = arguments.get("confirm_deletion");
            if (confirmObj != null && !(confirmObj instanceof Boolean)) {
                logger.warn("Validation failed: 'confirm_deletion' must be a boolean value");
                return ValidationResult.failure("'confirm_deletion' must be a boolean value");
            }
        }

        logger.debug("Validation successful for incident_id: {}", incidentId);
        return ValidationResult.success();
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> arguments) {
        String sessionId = context != null ? context.getSessionId() : "unknown";
        logger.info("Executing delete_incident tool with context: {}", sessionId);

        try {
            String incidentId = arguments.get("incident_id").toString().trim();
            boolean confirmDeletion = arguments.containsKey("confirm_deletion") && 
                                    Boolean.TRUE.equals(arguments.get("confirm_deletion"));

            logger.debug("Processing deletion request for incident '{}', confirmation: {}", incidentId, confirmDeletion);

            // Check if incident exists
            if (!incidentStorageService.incidentExists(incidentId)) {
                logger.warn("Deletion failed: incident '{}' not found", incidentId);
                Map<String, Object> errorResponse = Map.of(
                    "message", "Incident not found",
                    "incident_id", incidentId,
                    "deleted", false
                );
                String responseJson = objectMapper.writeValueAsString(errorResponse);
                return ToolResult.success(List.of(new McpSchema.TextContent(responseJson)));
            }

            if (!confirmDeletion) {
                // First step: Show incident details and request confirmation
                Map<String, Object> incident = incidentStorageService.getIncident(incidentId);
                
                Map<String, Object> confirmationResponse = Map.of(
                    "message", "CONFIRMATION REQUIRED: Please review the incident details below and confirm deletion",
                    "incident_details", incident,
                    "warning", "This action cannot be undone. The incident will be permanently deleted from storage.",
                    "next_step", "To proceed with deletion, call this tool again with the same incident_id and set confirm_deletion=true",
                    "deleted", false
                );
                
                String responseJson = objectMapper.writeValueAsString(confirmationResponse);
                logger.info("Displayed incident '{}' details for deletion confirmation", incidentId);
                return ToolResult.success(List.of(new McpSchema.TextContent(responseJson)));
            } else {
                // Second step: Perform actual deletion
                Map<String, Object> deletedIncident = incidentStorageService.deleteIncident(incidentId);
                
                Map<String, Object> deletionResponse = Map.of(
                    "message", "Incident successfully deleted",
                    "deleted_incident", deletedIncident,
                    "deleted", true
                );
                
                String responseJson = objectMapper.writeValueAsString(deletionResponse);
                logger.info("Successfully deleted incident '{}'", incidentId);
                return ToolResult.success(List.of(new McpSchema.TextContent(responseJson)));
            }

        } catch (Exception e) {
            logger.error("Failed to process deletion request for incident '{}': {}", 
                        arguments.get("incident_id"), e.getMessage(), e);
            return ToolResult.error("Failed to process deletion request: " + e.getMessage());
        }
    }
}
