package com.example.mcpserver.tool.api;

import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Objects;

/**
 * Represents the result of tool execution.
 * Contains the content returned by the tool and indicates whether an error occurred.
 */
public class ToolResult {
    private final List<McpSchema.Content> content;
    private final boolean isError;
    private final String errorMessage;

    private ToolResult(List<McpSchema.Content> content, boolean isError, String errorMessage) {
        this.content = content != null ? List.copyOf(content) : List.of();
        this.isError = isError;
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a successful tool result with the provided content.
     * 
     * @param content the content to return
     * @return a successful ToolResult
     */
    public static ToolResult success(List<McpSchema.Content> content) {
        return new ToolResult(content, false, null);
    }

    /**
     * Creates a successful tool result with a single text content.
     * 
     * @param text the text content to return
     * @return a successful ToolResult
     */
    public static ToolResult success(String text) {
        return success(List.of(new McpSchema.TextContent(text)));
    }

    /**
     * Creates an error tool result with the provided error message.
     * 
     * @param errorMessage the error message
     * @return an error ToolResult
     */
    public static ToolResult error(String errorMessage) {
        return new ToolResult(
            List.of(new McpSchema.TextContent("Error: " + errorMessage)), 
            true, 
            errorMessage
        );
    }

    /**
     * Creates an error tool result with the provided error message and content.
     * 
     * @param errorMessage the error message
     * @param content the error content to return
     * @return an error ToolResult
     */
    public static ToolResult error(String errorMessage, List<McpSchema.Content> content) {
        return new ToolResult(content, true, errorMessage);
    }

    /**
     * Returns the content produced by the tool execution.
     * 
     * @return the content list
     */
    public List<McpSchema.Content> getContent() {
        return content;
    }

    /**
     * Indicates whether this result represents an error.
     * 
     * @return true if this is an error result, false otherwise
     */
    public boolean isError() {
        return isError;
    }

    /**
     * Returns the error message if this is an error result.
     * 
     * @return the error message, or null if this is not an error result
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Indicates whether this result represents a successful execution.
     * 
     * @return true if this is a successful result, false otherwise
     */
    public boolean isSuccess() {
        return !isError;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolResult that = (ToolResult) o;
        return isError == that.isError &&
               Objects.equals(content, that.content) &&
               Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, isError, errorMessage);
    }

    @Override
    public String toString() {
        return "ToolResult{" +
               "content=" + content +
               ", isError=" + isError +
               ", errorMessage='" + errorMessage + '\'' +
               '}';
    }
}
