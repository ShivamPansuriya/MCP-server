package com.example.mcpserver.tool.factory;

/**
 * Exception thrown when tool creation fails.
 */
public class ToolCreationException extends RuntimeException {
    
    public ToolCreationException(String message) {
        super(message);
    }
    
    public ToolCreationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ToolCreationException(Throwable cause) {
        super(cause);
    }
}
