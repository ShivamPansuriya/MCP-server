package com.example.mcpserver.tool.registry;

/**
 * Represents the current status of a tool in the registry.
 */
public enum ToolStatus {
    /**
     * Tool is enabled and available for execution.
     */
    ENABLED,
    
    /**
     * Tool is disabled and not available for execution.
     */
    DISABLED,
    
    /**
     * Tool is currently being refreshed or reloaded.
     */
    REFRESHING,
    
    /**
     * Tool failed to initialize or has encountered an error.
     */
    ERROR,
    
    /**
     * Tool is in the process of being registered.
     */
    REGISTERING,
    
    /**
     * Tool is in the process of being unregistered.
     */
    UNREGISTERING
}
