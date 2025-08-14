package com.example.mcpserver.tool.api;

import com.example.mcpserver.tools.builtin.GetCurrentTimeTool;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GetCurrentTimeToolTest {

    private GetCurrentTimeTool tool;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        tool = new GetCurrentTimeTool();
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
        assertThat(metadata.getName()).isEqualTo("get_current_time");
        assertThat(metadata.getDescription()).isEqualTo("Returns the current date and time");
        assertThat(metadata.getVersion()).isEqualTo("1.0.0");
        assertThat(metadata.getCategory()).isEqualTo("utility");
        assertThat(metadata.getTags()).contains("time", "utility", "builtin");
    }

    @Test
    void shouldValidateEmptyArguments() {
        // When
        ValidationResult result = tool.validate(Map.of());

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldValidateValidFormat() {
        // When
        ValidationResult result1 = tool.validate(Map.of("format", "iso"));
        ValidationResult result2 = tool.validate(Map.of("format", "readable"));

        // Then
        assertThat(result1.isValid()).isTrue();
        assertThat(result2.isValid()).isTrue();
    }

    @Test
    void shouldRejectInvalidFormat() {
        // When
        ValidationResult result = tool.validate(Map.of("format", "invalid"));

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Format must be either 'iso' or 'readable'");
    }

    @Test
    void shouldRejectNonStringFormat() {
        // When
        ValidationResult result = tool.validate(Map.of("format", 123));

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Format parameter must be a string");
    }

    @Test
    void shouldExecuteWithReadableFormat() {
        // When
        ToolResult result = tool.execute(context, Map.of("format", "readable"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent()).hasSize(1);
        
        McpSchema.Content content = result.getContent().get(0);
        assertThat(content).isInstanceOf(McpSchema.TextContent.class);
        
        String text = ((McpSchema.TextContent) content).text();
        assertThat(text).startsWith("Current time: ");
        assertThat(text).matches("Current time: \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    void shouldExecuteWithIsoFormat() {
        // When
        ToolResult result = tool.execute(context, Map.of("format", "iso"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent()).hasSize(1);
        
        McpSchema.Content content = result.getContent().get(0);
        assertThat(content).isInstanceOf(McpSchema.TextContent.class);
        
        String text = ((McpSchema.TextContent) content).text();
        assertThat(text).startsWith("Current time: ");
        assertThat(text).matches("Current time: \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+");
    }

    @Test
    void shouldExecuteWithDefaultFormat() {
        // When
        ToolResult result = tool.execute(context, Map.of());

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent()).hasSize(1);
        
        McpSchema.Content content = result.getContent().get(0);
        assertThat(content).isInstanceOf(McpSchema.TextContent.class);
        
        String text = ((McpSchema.TextContent) content).text();
        assertThat(text).startsWith("Current time: ");
        // Should use readable format by default
        assertThat(text).matches("Current time: \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }
}
