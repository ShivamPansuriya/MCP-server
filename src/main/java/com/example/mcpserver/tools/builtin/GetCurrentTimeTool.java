package com.example.mcpserver.tools.builtin;

import com.example.mcpserver.tool.api.*;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tool that returns the current date and time.
 * Supports both readable and ISO format output.
 */
@Component
public class GetCurrentTimeTool implements McpTool {
    
    private static final String SCHEMA = """
        {
            "$schema": "http://json-schema.org/draft-07/schema#",
            "type": "object",
            "properties": {
                "format": {
                    "type": "string",
                    "description": "Optional time format: 'iso' for ISO format, 'readable' for human-readable format",
                    "enum": ["iso", "readable"]
                }
            },
            "required": []
        }
        """;
    
    @Override
    public String getName() {
        return "get_current_time";
    }
    
    @Override
    public String getDescription() {
        return "Returns the current date and time";
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
            .category("utility")
            .tags(Set.of("time", "utility", "builtin"))
            .schema(getSchema())
            .build();
    }
    
    @Override
    public ValidationResult validate(Map<String, Object> arguments) {
        if (arguments == null) {
            return ValidationResult.success();
        }
        
        Object formatObj = arguments.get("format");
        if (formatObj != null) {
            if (!(formatObj instanceof String)) {
                return ValidationResult.failure("Format parameter must be a string");
            }
            
            String format = (String) formatObj;
            if (!"iso".equals(format) && !"readable".equals(format)) {
                return ValidationResult.failure("Format must be either 'iso' or 'readable'");
            }
        }
        
        return ValidationResult.success();
    }
    
    @Override
    public ToolResult execute(McpSyncServerExchange exchange, ToolContext context, Map<String, Object> arguments) {
        try {
            String format = (String) arguments.getOrDefault("format", "readable");
            LocalDateTime now = LocalDateTime.now();
            String timeString;
            
            if ("iso".equals(format)) {
                timeString = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else {
                timeString = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            
            return ToolResult.success(
                List.of(new McpSchema.TextContent("Current time: " + timeString))
            );
        } catch (Exception e) {
            return ToolResult.error("Failed to get current time: " + e.getMessage());
        }
    }
}
