package com.example.mcpserver.tools.builtin;

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
 * Tool that returns structured metadata about incident fields that can be updated.
 * Provides detailed field information in SCIM-like format including type, mutability,
 * constraints, and validation rules. This tool should be called first to understand
 * the available fields before using update_incident.
 */
@Component
public class GetUpdatableFieldsTool implements McpTool {

    private static final Logger logger = LoggerFactory.getLogger(GetUpdatableFieldsTool.class);

    private final ObjectMapper objectMapper;

    private static final String SCHEMA = """
        {
            "$schema": "http://json-schema.org/draft-07/schema#",
            "type": "object",
            "description": "Get comprehensive metadata about fields that can be updated in incidents. No parameters required.",
            "properties": {},
            "additionalProperties": false
        }
        """;
    
    @Autowired
    public GetUpdatableFieldsTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "get_updatable_incident_fields";
    }
    
    @Override
    public String getDescription() {
        return "Returns detailed metadata about all incident fields that can be updated, including field types, validation rules, allowed values, and examples. Use this BEFORE calling update_incident to understand what fields are available and how to format them correctly.";
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
    public Mono<ToolResult> execute(McpAsyncServerExchange exchange, ToolContext context, Map<String, Object> arguments) {
        logger.info("Executing get_updatable_fields tool for session: {}", context.getSessionId());

        return Mono.fromCallable(() -> {
            // Define updatable fields with SCIM-like structured metadata
            List<Map<String, Object>> updatableFields = List.of(
                Map.of(
                    "name", "title",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Short title of the incident",
                    "maxLength", 200
                ),
                Map.of(
                    "name", "description",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Detailed description of the incident",
                    "maxLength", 4000
                ),
                Map.of(
                    "name", "priority",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Priority level of the incident",
                    "canonicalValues", List.of("Low", "Medium", "High", "Critical")
                ),
                Map.of(
                    "name", "status",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Status of the incident",
                    "canonicalValues", List.of("Open", "In Progress", "Resolved", "Closed")
                )
            );

            // Format response with structured attributes
            Map<String, Object> response = Map.of("attributes", updatableFields);
            String responseJson = objectMapper.writeValueAsString(response);

            logger.info("Successfully returned {} updatable field attributes", updatableFields.size());
            logger.debug("Updatable fields response: {}", responseJson);

            return ToolResult.success(
                List.of(new McpSchema.TextContent(responseJson))
            );
        })
        .doOnError(error -> logger.error("Failed to get updatable fields: {}", error.getMessage(), error))
        .onErrorResume(error -> Mono.just(ToolResult.error("Failed to get updatable fields: " + error.getMessage())));
    }
}
