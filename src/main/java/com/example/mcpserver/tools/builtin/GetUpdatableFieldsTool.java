package com.example.mcpserver.tools.builtin;

import com.example.mcpserver.tool.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tool that returns metadata about which incident fields can be updated.
 * Provides field information including type and description for each updatable field.
 */
@Component
public class GetUpdatableFieldsTool implements McpTool {

    private static final Logger logger = LoggerFactory.getLogger(GetUpdatableFieldsTool.class);

    private final ObjectMapper objectMapper;
    
    private static final String SCHEMA = """
        {
            "$schema": "http://json-schema.org/draft-07/schema#",
            "type": "object",
            "properties": {},
            "required": []
        }
        """;
    
    @Autowired
    public GetUpdatableFieldsTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "get_updatable_fields";
    }
    
    @Override
    public String getDescription() {
        return "Return the list of fields that can be updated for an incident, including their type and description";
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
        logger.debug("Validating get_updatable_fields arguments (no parameters required)");
        // No parameters required for this tool
        return ValidationResult.success();
    }
    
    @Override
    public ToolResult execute(McpSyncServerExchange exchange, ToolContext context, Map<String, Object> arguments) {
        logger.info("Executing get_updatable_fields tool for session: {}", context.getSessionId());

        try {
            // Define updatable fields with their metadata
            List<Map<String, String>> updatableFields = List.of(
                Map.of(
                    "field_name", "title",
                    "field_type", "string",
                    "description", "Short title of the incident."
                ),
                Map.of(
                    "field_name", "description",
                    "field_type", "string",
                    "description", "Detailed description of the incident."
                ),
                Map.of(
                    "field_name", "priority",
                    "field_type", "string",
                    "description", "Priority level: Low, Medium, High, Critical."
                ),
                Map.of(
                    "field_name", "status",
                    "field_type", "string",
                    "description", "Status of the incident: Open, In Progress, Resolved, Closed."
                )
            );
            
            // Format response
            Map<String, Object> response = Map.of("updatable_fields", updatableFields);
            String responseJson = objectMapper.writeValueAsString(response);

            logger.info("Successfully returned {} updatable fields", updatableFields.size());
            logger.debug("Updatable fields response: {}", responseJson);

            return ToolResult.success(
                List.of(new McpSchema.TextContent(responseJson))
            );

        } catch (Exception e) {
            logger.error("Failed to get updatable fields: {}", e.getMessage(), e);
            return ToolResult.error("Failed to get updatable fields: " + e.getMessage());
        }
    }
}
