package com.example.mcpserver.tool.api;

import java.util.Map;

/**
 * Core interface for MCP tools.
 * All tools must implement this interface to be registered and executed by the MCP server.
 */
public interface McpTool {
    
    /**
     * Returns the unique name of this tool.
     * This name is used for tool registration and client requests.
     * 
     * @return the tool name
     */
    String getName();
    
    /**
     * Returns a human-readable description of what this tool does.
     * 
     * @return the tool description
     */
    String getDescription();
    
    /**
     * Returns the JSON schema that defines the tool's input parameters.
     * This schema is used for client-side validation and documentation.
     * 
     * @return the JSON schema as a string
     */
    String getSchema();
    
    /**
     * Returns comprehensive metadata about this tool.
     * 
     * @return the tool metadata
     */
    ToolMetadata getMetadata();
    
    /**
     * Validates the provided arguments against the tool's schema and requirements.
     * 
     * @param arguments the arguments to validate
     * @return validation result indicating success or failure with details
     */
    ValidationResult validate(Map<String, Object> arguments);
    
    /**
     * Executes the tool with the provided context and arguments.
     * 
     * @param context the execution context containing session and request information
     * @param arguments the tool arguments
     * @return the execution result
     */
    ToolResult execute(ToolContext context, Map<String, Object> arguments);
}
