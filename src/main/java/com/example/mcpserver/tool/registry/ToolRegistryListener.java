package com.example.mcpserver.tool.registry;

/**
 * Listener interface for tool registry events.
 * Implementations can react to tool registration, unregistration, and other registry changes.
 */
public interface ToolRegistryListener {
    
    /**
     * Called when a tool is successfully registered in the registry.
     * 
     * @param definition the registered tool definition
     */
    default void onToolRegistered(ToolDefinition definition) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a tool is unregistered from the registry.
     * 
     * @param definition the unregistered tool definition
     */
    default void onToolUnregistered(ToolDefinition definition) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a tool's status changes.
     * 
     * @param definition the tool definition
     * @param oldStatus the previous status
     * @param newStatus the new status
     */
    default void onToolStatusChanged(ToolDefinition definition, ToolStatus oldStatus, ToolStatus newStatus) {
        // Default implementation does nothing
    }
    
    /**
     * Called when the registry is cleared.
     */
    default void onRegistryCleared() {
        // Default implementation does nothing
    }
    
    /**
     * Called when a tool is accessed (looked up) from the registry.
     * 
     * @param definition the accessed tool definition
     */
    default void onToolAccessed(ToolDefinition definition) {
        // Default implementation does nothing
    }
}
