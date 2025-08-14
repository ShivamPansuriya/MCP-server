package com.example.mcpserver.tool.api;

import com.example.mcpserver.tools.builtin.EchoTool;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EchoToolTest {

    private EchoTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new EchoTool();
        context = ToolContext.builder()
            .sessionId("test-session")
            .requestId("test-request")
            .build();
    }

    @Test
    void shouldReturnCorrectMetadata() {
        // When
        ToolMetadata metadata = tool.getMetadata();

        // Then
        assertThat(metadata.getName()).isEqualTo("echo");
        assertThat(metadata.getDescription()).isEqualTo("Echoes back the provided text");
        assertThat(metadata.getVersion()).isEqualTo("1.0.0");
        assertThat(metadata.getCategory()).isEqualTo("utility");
        assertThat(metadata.getTags()).contains("text", "utility", "builtin");
    }

    @Test
    void shouldRejectNullArguments() {
        // When
        ValidationResult result = tool.validate(null);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Arguments cannot be null");
    }

    @Test
    void shouldRejectMissingTextParameter() {
        // When
        ValidationResult result = tool.validate(Map.of());

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("'text' parameter is required");
    }

    @Test
    void shouldRejectNonStringTextParameter() {
        // When
        ValidationResult result = tool.validate(Map.of("text", 123));

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("'text' parameter must be a string");
    }

    @Test
    void shouldWarnAboutEmptyText() {
        // When
        ValidationResult result = tool.validate(Map.of("text", "   "));

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.hasWarnings()).isTrue();
        assertThat(result.getWarnings()).contains("Text parameter is empty");
    }

    @Test
    void shouldValidateValidText() {
        // When
        ValidationResult result = tool.validate(Map.of("text", "Hello, World!"));

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.hasWarnings()).isFalse();
    }

    @Test
    void shouldExecuteSuccessfully() {
        // Given
        String inputText = "Hello, MCP!";

        // When
        ToolResult result = tool.execute(context, Map.of("text", inputText));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent()).hasSize(1);
        
        McpSchema.Content content = result.getContent().get(0);
        assertThat(content).isInstanceOf(McpSchema.TextContent.class);
        
        String text = ((McpSchema.TextContent) content).text();
        assertThat(text).isEqualTo("Echo: " + inputText);
    }

    @Test
    void shouldHandleMissingTextParameter() {
        // When
        ToolResult result = tool.execute(context, Map.of());

        // Then
        assertThat(result.isError()).isTrue();
        assertThat(result.getErrorMessage()).isEqualTo("'text' parameter is required");
    }

    @Test
    void shouldHandleSpecialCharacters() {
        // Given
        String inputText = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";

        // When
        ToolResult result = tool.execute(context, Map.of("text", inputText));

        // Then
        assertThat(result.isSuccess()).isTrue();
        
        McpSchema.Content content = result.getContent().get(0);
        String text = ((McpSchema.TextContent) content).text();
        assertThat(text).isEqualTo("Echo: " + inputText);
    }

    @Test
    void shouldHandleUnicodeText() {
        // Given
        String inputText = "Unicode: 你好世界 🌍 🚀";

        // When
        ToolResult result = tool.execute(context, Map.of("text", inputText));

        // Then
        assertThat(result.isSuccess()).isTrue();
        
        McpSchema.Content content = result.getContent().get(0);
        String text = ((McpSchema.TextContent) content).text();
        assertThat(text).isEqualTo("Echo: " + inputText);
    }
}
