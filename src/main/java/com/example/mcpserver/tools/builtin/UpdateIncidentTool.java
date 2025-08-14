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
 * Tool for updating existing incident fields.
 * Allows updating title, description, priority, and status of existing incidents.
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
                "incident_id": {
                    "type": "string",
                    "description": "ID of the incident to update (e.g., INC-1234ABCD).",
                    "pattern": "^INC-[A-F0-9]{8}$"
                },
                "updates": {
                    "type": "object",
                    "description": "Dictionary of fields to update and their new values.",
                    "properties": {
                        "title": {
                            "type": "string",
                            "description": "New title for the incident",
                            "maxLength": 200
                        },
                        "description": {
                            "type": "string",
                            "description": "New description for the incident",
                            "maxLength": 2000
                        },
                        "priority": {
                            "type": "string",
                            "description": "New priority level",
                            "enum": ["Low", "Medium", "High", "Critical"]
                        },
                        "status": {
                            "type": "string",
                            "description": "New status",
                            "enum": ["Open", "In Progress", "Resolved", "Closed"]
                        }
                    },
                    "additionalProperties": false,
                    "minProperties": 1
                }
            },
            "required": ["incident_id", "updates"]
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
        return "Update fields of an existing incident. The 'updates' dict can contain any valid field from get_updatable_fields";
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
        if (!arguments.containsKey("incident_id") || arguments.get("incident_id") == null) {
            logger.warn("Validation failed: missing 'incident_id' parameter");
            return ValidationResult.failure("'incident_id' parameter is required");
        }

        if (!arguments.containsKey("updates") || arguments.get("updates") == null) {
            logger.warn("Validation failed: missing 'updates' parameter");
            return ValidationResult.failure("'updates' parameter is required");
        }
        
        String incidentId = arguments.get("incident_id").toString().trim();
        if (incidentId.isEmpty()) {
            logger.warn("Validation failed: empty 'incident_id' parameter");
            return ValidationResult.failure("'incident_id' cannot be empty");
        }

        // Validate incident ID format
        if (!incidentId.matches("^INC-[A-F0-9]{8}$")) {
            logger.warn("Validation failed: invalid incident_id format '{}'. Expected format: INC-XXXXXXXX", incidentId);
            return ValidationResult.failure("'incident_id' must be in format INC-XXXXXXXX where X is a hex digit");
        }
        
        // Validate updates object
        Object updatesObj = arguments.get("updates");
        if (!(updatesObj instanceof Map)) {
            logger.warn("Validation failed: 'updates' parameter is not an object, got: {}", updatesObj.getClass().getSimpleName());
            return ValidationResult.failure("'updates' must be an object");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> updates = (Map<String, Object>) updatesObj;

        if (updates.isEmpty()) {
            logger.warn("Validation failed: 'updates' parameter is empty");
            return ValidationResult.failure("'updates' cannot be empty");
        }

        logger.debug("Validating {} update fields for incident: {}", updates.size(), incidentId);
        
        // Validate each update field
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            if (!IncidentStorageService.UPDATABLE_FIELDS.contains(field)) {
                logger.warn("Validation failed: invalid field '{}'. Valid fields: {}", field, IncidentStorageService.UPDATABLE_FIELDS);
                return ValidationResult.failure("Invalid field '" + field + "'. Valid fields are: " +
                    String.join(", ", IncidentStorageService.UPDATABLE_FIELDS));
            }

            if (value == null) {
                logger.warn("Validation failed: field '{}' has null value", field);
                return ValidationResult.failure("Field '" + field + "' cannot have null value");
            }
            
            String stringValue = value.toString();
            
            // Validate specific field constraints
            switch (field) {
                case "title":
                    if (stringValue.trim().isEmpty()) {
                        logger.warn("Validation failed: empty 'title' value");
                        return ValidationResult.failure("'title' cannot be empty");
                    }
                    if (stringValue.length() > 200) {
                        logger.warn("Validation failed: 'title' length {} exceeds 200 characters", stringValue.length());
                        return ValidationResult.failure("'title' cannot exceed 200 characters");
                    }
                    break;
                case "description":
                    if (stringValue.length() > 2000) {
                        logger.warn("Validation failed: 'description' length {} exceeds 2000 characters", stringValue.length());
                        return ValidationResult.failure("'description' cannot exceed 2000 characters");
                    }
                    break;
                case "priority":
                    if (!incidentStorageService.isValidPriority(stringValue)) {
                        logger.warn("Validation failed: invalid priority '{}'. Valid priorities: {}", stringValue, IncidentStorageService.VALID_PRIORITIES);
                        return ValidationResult.failure("'priority' must be one of: Low, Medium, High, Critical");
                    }
                    break;
                case "status":
                    if (!incidentStorageService.isValidStatus(stringValue)) {
                        logger.warn("Validation failed: invalid status '{}'. Valid statuses: {}", stringValue, IncidentStorageService.VALID_STATUSES);
                        return ValidationResult.failure("'status' must be one of: Open, In Progress, Resolved, Closed");
                    }
                    break;
            }
        }

        logger.debug("Validation successful for update_incident with incident_id: '{}' and {} fields", incidentId, updates.size());
        return ValidationResult.success();
    }
    
    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> arguments) {
        logger.info("Executing update_incident tool for session: {}", context.getSessionId());

        try {
            String incidentId = arguments.get("incident_id").toString().trim();

            @SuppressWarnings("unchecked")
            Map<String, Object> updates = (Map<String, Object>) arguments.get("updates");

            logger.debug("Attempting to update incident '{}' with fields: {}", incidentId, updates.keySet());

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
            
            // Update the incident
            Map<String, Object> updatedIncident = incidentStorageService.updateIncident(incidentId, updates);

            logger.info("Successfully updated incident '{}' with {} fields", incidentId, updates.size());
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

        } catch (Exception e) {
            logger.error("Failed to update incident '{}': {}", arguments.get("incident_id"), e.getMessage(), e);
            return ToolResult.error("Failed to update incident: " + e.getMessage());
        }
    }
}
