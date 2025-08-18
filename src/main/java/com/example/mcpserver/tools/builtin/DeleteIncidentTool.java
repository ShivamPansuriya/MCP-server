package com.example.mcpserver.tools.builtin;

import com.example.mcpserver.service.IncidentStorageService;
import com.example.mcpserver.tool.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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
        return "Safely delete an incident with user confirmation via elicitation. Shows incident details and requests explicit user confirmation before deletion.";
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

        logger.debug("Validation successful for incident_id: {}", incidentId);
        return ValidationResult.success();
    }

    @Override
    public ToolResult execute(McpSyncServerExchange exchange, ToolContext context, Map<String, Object> arguments) {
        String sessionId = context != null ? context.getSessionId() : "unknown";
        logger.info("Executing delete_incident tool with context: {}", sessionId);

        try {
            String incidentId = arguments.get("incident_id").toString().trim();
            logger.debug("Processing deletion request for incident '{}'", incidentId);

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

            // Get incident details for confirmation
            Map<String, Object> incident = incidentStorageService.getIncident(incidentId);

            // Check if client supports elicitation
            if (exchange.getClientCapabilities() == null || exchange.getClientCapabilities().elicitation() == null) {
                logger.warn("Client does not support elicitation capabilities");
                Map<String, Object> errorResponse = Map.of(
                    "message", "Client does not support user confirmation. Cannot safely delete incident.",
                    "incident_id", incidentId,
                    "deleted", false
                );
                String responseJson = objectMapper.writeValueAsString(errorResponse);
                return ToolResult.success(List.of(new McpSchema.TextContent(responseJson)));
            }

            // Create elicitation request for user confirmation
            String confirmationMessage = String.format(
                "⚠️ DELETION CONFIRMATION REQUIRED ⚠️\n\n" +
                "You are about to permanently delete incident: %s\n\n" +
                "Incident Details:\n" +
                "- ID: %s\n" +
                "- Title: %s\n" +
                "- Status: %s\n" +
                "- Priority: %s\n" +
                "- Created: %s\n\n" +
                "⚠️ This action cannot be undone! ⚠️\n\n" +
                "Do you want to proceed with the deletion?",
                incidentId,
                incident.get("id"),
                incident.get("title"),
                incident.get("status"),
                incident.get("priority"),
                incident.get("created_at")
            );

            // Create simple confirmation schema
            Map<String, Object> confirmationSchema = new HashMap<>();
            confirmationSchema.put("type", "object");
            Map<String, Object> properties = new HashMap<>();
            Map<String, Object> confirmProperty = new HashMap<>();
            confirmProperty.put("type", "boolean");
            confirmProperty.put("title", "Confirm Deletion");
            confirmProperty.put("description", "Check this box to confirm you want to delete this incident");
            confirmProperty.put("default", false);
            properties.put("confirm", confirmProperty);
            confirmationSchema.put("properties", properties);
            confirmationSchema.put("required", List.of("confirm"));

            McpSchema.ElicitRequest elicitRequest = McpSchema.ElicitRequest.builder()
                .message(confirmationMessage)
                .requestedSchema(confirmationSchema)
                .build();

            // Request user confirmation via elicitation
            logger.info("Requesting user confirmation for deletion of incident '{}'", incidentId);
            McpSchema.ElicitResult elicitResult = exchange.createElicitation(elicitRequest);

            // Handle elicitation result
            switch (elicitResult.action()) {
                case ACCEPT:
                    // User confirmed - check if they actually confirmed
                    if (elicitResult.content() != null &&
                        Boolean.TRUE.equals(elicitResult.content().get("confirm"))) {

                        // Perform actual deletion
                        Map<String, Object> deletedIncident = incidentStorageService.deleteIncident(incidentId);

                        Map<String, Object> deletionResponse = Map.of(
                            "message", "Incident successfully deleted",
                            "deleted_incident", deletedIncident,
                            "deleted", true
                        );

                        String responseJson = objectMapper.writeValueAsString(deletionResponse);
                        logger.info("Successfully deleted incident '{}' after user confirmation", incidentId);
                        return ToolResult.success(List.of(new McpSchema.TextContent(responseJson)));
                    } else {
                        // User submitted but didn't confirm
                        Map<String, Object> cancelResponse = Map.of(
                            "message", "Deletion cancelled - confirmation checkbox was not checked",
                            "incident_id", incidentId,
                            "deleted", false
                        );
                        String responseJson = objectMapper.writeValueAsString(cancelResponse);
                        logger.info("Deletion cancelled for incident '{}' - user did not confirm", incidentId);
                        return ToolResult.success(List.of(new McpSchema.TextContent(responseJson)));
                    }

                case DECLINE:
                    // User explicitly declined
                    Map<String, Object> declineResponse = Map.of(
                        "message", "Deletion declined by user",
                        "incident_id", incidentId,
                        "deleted", false
                    );
                    String responseJson = objectMapper.writeValueAsString(declineResponse);
                    logger.info("Deletion declined by user for incident '{}'", incidentId);
                    return ToolResult.success(List.of(new McpSchema.TextContent(responseJson)));

                case CANCEL:
                default:
                    // User cancelled or dismissed
                    Map<String, Object> cancelResponse = Map.of(
                        "message", "Deletion cancelled by user",
                        "incident_id", incidentId,
                        "deleted", false
                    );
                    String cancelResponseJson = objectMapper.writeValueAsString(cancelResponse);
                    logger.info("Deletion cancelled by user for incident '{}'", incidentId);
                    return ToolResult.success(List.of(new McpSchema.TextContent(cancelResponseJson)));
            }

        } catch (Exception e) {
            logger.error("Failed to process deletion request for incident '{}': {}",
                        arguments.get("incident_id"), e.getMessage(), e);
            return ToolResult.error("Failed to process deletion request: " + e.getMessage());
        }
    }
}
