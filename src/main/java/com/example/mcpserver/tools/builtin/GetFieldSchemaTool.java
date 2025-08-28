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
 * Tool that returns structured metadata about fields that can be updated for different models.
 * Provides detailed field information in SCIM-like format including type, mutability,
 * constraints, and validation rules. This tool should be called first to understand
 * the available fields before using update operations.
 */
@Component
public class GetFieldSchemaTool implements McpTool {

    private static final Logger logger = LoggerFactory.getLogger(GetFieldSchemaTool.class);

    private final ObjectMapper objectMapper;

    private static final String SCHEMA = """
        {
            "$schema": "http://json-schema.org/draft-07/schema#",
            "type": "object",
            "description": "Get comprehensive metadata about fields that can be updated for a specific model type.",
            "properties": {
                "model": {
                    "type": "string",
                    "description": "The model type to get updatable fields for",
                    "enum": ["request", "service_catalog", "problem"]
                }
            },
            "required": ["model"],
            "additionalProperties": false
        }
        """;
    
    @Autowired
    public GetFieldSchemaTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "get_fields_schema";
    }
    
    @Override
    public String getDescription() {
        return "Returns detailed metadata about all fields that can be updated for a specific model type (request, service_catalog, or problem), including field types, validation rules, allowed values, and examples. Use this BEFORE calling update operations to understand what fields are available and how to format them correctly.";
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
        logger.debug("Validating get_fields_schema arguments");

        if (arguments == null || arguments.isEmpty()) {
            return ValidationResult.failure("Model parameter is required");
        }

        Object modelObj = arguments.get("model");
        if (modelObj == null) {
            return ValidationResult.failure("Model parameter is required");
        }

        String model = modelObj.toString();
        if (!List.of("request", "service_catalog", "problem").contains(model)) {
            return ValidationResult.failure("Model must be one of: request, service_catalog, problem");
        }

        return ValidationResult.success();
    }
    
    @Override
    public Mono<ToolResult> execute(McpAsyncServerExchange exchange, ToolContext context, Map<String, Object> arguments) {
        String model = arguments.get("model").toString();
        logger.info("Executing get_fields_schema tool for model: {} and session: {}", model, context.getSessionId());

        return Mono.fromCallable(() -> {
            // Get fields based on model type
            List<Map<String, Object>> fields = getFieldsForModel(model);

            // Format response with structured attributes
            Map<String, Object> response = Map.of(
                "model", model,
                "attributes", FieldConstants.INCIDENT_FIELDS_SCHEMA
            );
            String responseJson = objectMapper.writeValueAsString(response);

            logger.info("Successfully returned {} form field attributes for model: {}", fields.size(), model);
            logger.debug("Form fields response: {}", responseJson);

            return ToolResult.success(
                List.of(new McpSchema.TextContent(responseJson))
            );
        })
        .doOnError(error -> logger.error("Failed to get form fields for model {}: {}", model, error.getMessage(), error))
        .onErrorResume(error -> Mono.just(ToolResult.error("Failed to get form fields for model " + model + ": " + error.getMessage())));
    }

    private List<Map<String, Object>> getFieldsForModel(String model) {
        return switch (model) {
            case "request" -> FieldConstants.UPDATABLE_FIELDS; // Current incident fields
//            case "service_catalog" -> FieldConstants.SERVICE_CATALOG_FIELDS;
//            case "problem" -> FieldConstants.PROBLEM_FIELDS;
            default -> throw new IllegalArgumentException("Unsupported model: " + model);
        };
    }
}
