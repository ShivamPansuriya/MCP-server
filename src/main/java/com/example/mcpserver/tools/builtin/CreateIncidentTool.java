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
 * Tool for creating new incident requests.
 * Creates incidents with proper field validation and unique ID generation.
 */
@Component
public class CreateIncidentTool implements McpTool {

    private static final Logger logger = LoggerFactory.getLogger(CreateIncidentTool.class);

    private final IncidentStorageService incidentStorageService;
    private final ObjectMapper objectMapper;
    
    private static final String SCHEMA = """
        {
            "$schema": "http://json-schema.org/draft-07/schema#",
            "type": "object",
            "properties": {
                "title": {
                    "type": "string",
                    "description": "Short title of the incident. Mandatory.",
                    "minLength": 1,
                    "maxLength": 200
                },
                "requester": {
                    "type": "string",
                    "description": "Name or email of the person raising the incident. Mandatory.",
                    "minLength": 1,
                    "maxLength": 200
                },
                "description": {
                    "type": "string",
                    "description": "Detailed description of the issue. Optional.",
                    "maxLength": 4000
                },
                "priority": {
                    "type": "string",
                    "description": "Priority level of the incident. Options: Low, Medium, High, Critical.",
                    "enum": ["Low", "Medium", "High", "Critical"]
                }
            },
            "required": ["title", "requester"]
        }
        """;
    
    @Autowired
    public CreateIncidentTool(IncidentStorageService incidentStorageService, ObjectMapper objectMapper) {
        this.incidentStorageService = incidentStorageService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "create_incident";
    }
    
    @Override
    public String getDescription() {
        return "Create an incident request with proper field schema";
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
        logger.debug("Validating create_incident arguments: {}", arguments.keySet());

        // Check required fields
        if (!arguments.containsKey("title") || arguments.get("title") == null) {
            logger.warn("Validation failed: missing 'title' parameter");
            return ValidationResult.failure("'title' parameter is required");
        }

        if (!arguments.containsKey("requester") || arguments.get("requester") == null) {
            logger.warn("Validation failed: missing 'requester' parameter");
            return ValidationResult.failure("'requester' parameter is required");
        }
        
        String title = arguments.get("title").toString().trim();
        String requester = arguments.get("requester").toString().trim();
        
        if (title.isEmpty()) {
            logger.warn("Validation failed: empty 'title' parameter");
            return ValidationResult.failure("'title' cannot be empty");
        }

        if (requester.isEmpty()) {
            logger.warn("Validation failed: empty 'requester' parameter");
            return ValidationResult.failure("'requester' cannot be empty");
        }

        // Validate title length
        if (title.length() > 200) {
            logger.warn("Validation failed: 'title' length {} exceeds 200 characters", title.length());
            return ValidationResult.failure("'title' cannot exceed 200 characters");
        }

        // Validate requester length
        if (requester.length() > 100) {
            logger.warn("Validation failed: 'requester' length {} exceeds 100 characters", requester.length());
            return ValidationResult.failure("'requester' cannot exceed 100 characters");
        }
        
        // Validate description length if provided
        if (arguments.containsKey("description") && arguments.get("description") != null) {
            String description = arguments.get("description").toString();
            if (description.length() > 2000) {
                logger.warn("Validation failed: 'description' length {} exceeds 2000 characters", description.length());
                return ValidationResult.failure("'description' cannot exceed 2000 characters");
            }
        }

        // Validate priority if provided
        if (arguments.containsKey("priority") && arguments.get("priority") != null) {
            String priority = arguments.get("priority").toString();
            if (!incidentStorageService.isValidPriority(priority)) {
                logger.warn("Validation failed: invalid priority '{}'. Valid priorities: {}", priority, IncidentStorageService.VALID_PRIORITIES);
                return ValidationResult.failure("'priority' must be one of: Low, Medium, High, Critical");
            }
        }

        logger.debug("Validation successful for create_incident with title: '{}'", title);
        return ValidationResult.success();
    }
    
    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> arguments) {
        logger.info("Executing create_incident tool for session: {}", context.getSessionId());

        try {
            // Extract parameters
            String title = arguments.get("title").toString().trim();
            String requester = arguments.get("requester").toString().trim();
            String description = arguments.containsKey("description") && arguments.get("description") != null
                ? arguments.get("description").toString() : "";
            String priority = arguments.containsKey("priority") && arguments.get("priority") != null
                ? arguments.get("priority").toString() : "Medium";

            logger.debug("Creating incident with title: '{}', requester: '{}', priority: '{}'", title, requester, priority);

            // Create incident
            Map<String, Object> incident = incidentStorageService.createIncident(title, requester, description, priority);
            String incidentId = (String) incident.get("incident_id");

            logger.info("Successfully created incident with ID: {}", incidentId);
            
            // Format response
            String responseMessage = String.format("Incident created successfully");
            String responseJson = objectMapper.writeValueAsString(Map.of(
                "message", responseMessage,
                "incident_id", incident.get("incident_id"),
                "title", incident.get("title"),
                "requester", incident.get("requester"),
                "description", incident.get("description"),
                "priority", incident.get("priority"),
                "status", incident.get("status"),
                "created_at", incident.get("created_at")
            ));
            
            logger.debug("Returning successful response for incident: {}", incidentId);
            return ToolResult.success(
                List.of(new McpSchema.TextContent(responseJson))
            );

        } catch (Exception e) {
            logger.error("Failed to create incident: {}", e.getMessage(), e);
            return ToolResult.error("Failed to create incident: " + e.getMessage());
        }
    }
}
