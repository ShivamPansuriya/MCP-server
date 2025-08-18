package com.example.mcpserver.tools.builtin;

import com.example.mcpserver.tool.api.*;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tool that echoes back the provided text.
 * Demonstrates basic text processing and parameter validation.
 */
@Component
public class EchoTool implements McpTool {
    
    private static final String SCHEMA = """
        {
            "$schema": "http://json-schema.org/draft-07/schema#",
            "type": "object",
            "properties": {
                "text": {
                    "type": "string",
                    "description": "Text to echo back"
                }
            },
            "required": ["text"]
        }
        """;
    
    @Override
    public String getName() {
        return "echo";
    }
    
    @Override
    public String getDescription() {
        return "Echoes back the provided text";
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
            .tags(Set.of("text", "utility", "builtin"))
            .schema(getSchema())
            .build();
    }
    
    @Override
    public ValidationResult validate(Map<String, Object> arguments) {
        if (arguments == null) {
            return ValidationResult.failure("Arguments cannot be null");
        }
        
        Object textObj = arguments.get("text");
        if (textObj == null) {
            return ValidationResult.failure("'text' parameter is required");
        }
        
        if (!(textObj instanceof String)) {
            return ValidationResult.failure("'text' parameter must be a string");
        }
        
        String text = (String) textObj;
        if (text.trim().isEmpty()) {
            return ValidationResult.successWithWarnings(List.of("Text parameter is empty"));
        }
        
        return ValidationResult.success();
    }
    
    @Override
    public Mono<ToolResult> execute(McpAsyncServerExchange exchange, ToolContext context, Map<String, Object> arguments) {
        return Mono.fromCallable(() -> {
            String text = (String) arguments.get("text");

            if (text == null) {
                throw new IllegalArgumentException("'text' parameter is required");
            }

            return ToolResult.success(
                List.of(new McpSchema.TextContent("Echo: " + text))
            );
        })
        .onErrorResume(error -> {
            String errorMessage = error instanceof IllegalArgumentException ?
                    error.getMessage() : "Failed to echo text: " + error.getMessage();
            return Mono.just(ToolResult.error(errorMessage));
        });
    }
}
