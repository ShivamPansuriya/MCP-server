package com.example.mcpserver.tool.factory;

import com.example.mcpserver.tool.api.McpTool;
import com.example.mcpserver.tool.registry.ToolDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of ToolFactory using Spring's ApplicationContext.
 * Creates tool instances using Spring's dependency injection.
 */
@Component
public class DefaultToolFactory implements ToolFactory {
    private static final Logger logger = LoggerFactory.getLogger(DefaultToolFactory.class);
    
    private final ApplicationContext applicationContext;
    private final ConcurrentHashMap<String, McpTool> toolCache = new ConcurrentHashMap<>();
    
    public DefaultToolFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public McpTool createTool(ToolDefinition definition) throws ToolCreationException {
        try {
            Class<? extends McpTool> toolClass = definition.getToolClass();
            String toolName = definition.getMetadata().getName();
            
            // Check cache first
            McpTool cachedTool = toolCache.get(toolName);
            if (cachedTool != null) {
                logger.debug("Returning cached tool instance for: {}", toolName);
                return cachedTool;
            }
            
            // Create new instance using Spring context
            McpTool tool = applicationContext.getBean(toolClass);
            
            // Cache the tool instance
            toolCache.put(toolName, tool);
            
            logger.debug("Created new tool instance for: {}", toolName);
            return tool;
            
        } catch (Exception e) {
            String errorMsg = "Failed to create tool: " + definition.getMetadata().getName();
            logger.error(errorMsg, e);
            throw new ToolCreationException(errorMsg, e);
        }
    }
    
    @Override
    public boolean supports(Class<? extends McpTool> toolClass) {
        try {
            String[] beanNames = applicationContext.getBeanNamesForType(toolClass);
            return beanNames.length > 0;
        } catch (Exception e) {
            logger.debug("Error checking support for tool class {}: {}", toolClass.getName(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public McpTool getTool(ToolDefinition definition) throws ToolCreationException {
        return createTool(definition);
    }
    
    @Override
    public void destroyTool(McpTool tool) {
        if (tool != null) {
            String toolName = tool.getName();
            toolCache.remove(toolName);
            logger.debug("Destroyed tool instance for: {}", toolName);
        }
    }
    
    /**
     * Clears the tool cache.
     * Forces recreation of all tool instances on next access.
     */
    public void clearCache() {
        logger.info("Clearing tool factory cache");
        toolCache.clear();
    }
    
    /**
     * Returns the number of cached tool instances.
     * 
     * @return the cache size
     */
    public int getCacheSize() {
        return toolCache.size();
    }
}
