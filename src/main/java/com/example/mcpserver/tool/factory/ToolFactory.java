package com.example.mcpserver.tool.factory;

import com.example.mcpserver.tool.api.McpTool;
import com.example.mcpserver.tool.registry.ToolDefinition;

/**
 * Factory interface for creating MCP tool instances.
 * Implementations handle the creation and lifecycle of tool instances.
 */
public interface ToolFactory {
    
    /**
     * Creates a tool instance from the given tool definition.
     * 
     * @param definition the tool definition
     * @return the created tool instance
     * @throws ToolCreationException if tool creation fails
     */
    McpTool createTool(ToolDefinition definition) throws ToolCreationException;
    
    /**
     * Checks if this factory supports creating instances of the given tool class.
     * 
     * @param toolClass the tool class to check
     * @return true if this factory can create instances of the tool class
     */
    boolean supports(Class<? extends McpTool> toolClass);
    
    /**
     * Gets the tool instance for the given definition.
     * This may return a cached instance or create a new one depending on the implementation.
     * 
     * @param definition the tool definition
     * @return the tool instance
     * @throws ToolCreationException if tool creation fails
     */
    default McpTool getTool(ToolDefinition definition) throws ToolCreationException {
        return createTool(definition);
    }
    
    /**
     * Destroys a tool instance and cleans up any resources.
     * 
     * @param tool the tool instance to destroy
     */
    default void destroyTool(McpTool tool) {
        // Default implementation does nothing
    }
}
