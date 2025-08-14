package com.example.mcpserver.tool.discovery;

import com.example.mcpserver.tool.api.McpTool;
import com.example.mcpserver.tool.registry.ToolDefinition;
import com.example.mcpserver.tool.registry.ToolRegistry;
import com.example.mcpserver.tool.registry.ToolStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * Service responsible for discovering and registering MCP tools.
 * Automatically discovers Spring-managed tool beans and registers them in the tool registry.
 */
@Service
public class ToolDiscoveryService {
    private static final Logger logger = LoggerFactory.getLogger(ToolDiscoveryService.class);
    
    private final ToolRegistry toolRegistry;
    private final ApplicationContext applicationContext;
    
    @Autowired
    public ToolDiscoveryService(ToolRegistry toolRegistry, ApplicationContext applicationContext) {
        this.toolRegistry = toolRegistry;
        this.applicationContext = applicationContext;
    }
    
    /**
     * Discovers and registers tools automatically on startup.
     */
    @PostConstruct
    public void discoverTools() {
        logger.info("Starting tool discovery process");
        
        try {
            // Discover Spring-managed tools
            Map<String, McpTool> toolBeans = applicationContext.getBeansOfType(McpTool.class);
            logger.info("Found {} tool beans in Spring context", toolBeans.size());
            
            for (Map.Entry<String, McpTool> entry : toolBeans.entrySet()) {
                String beanName = entry.getKey();
                McpTool tool = entry.getValue();
                
                try {
                    ToolDefinition definition = createDefinitionFromTool(tool);
                    toolRegistry.register(definition);
                    logger.info("Successfully registered tool: {} (bean: {})", tool.getName(), beanName);
                } catch (Exception e) {
                    logger.error("Failed to register tool from bean {}: {}", beanName, e.getMessage(), e);
                }
            }
            
            logger.info("Tool discovery completed. Registered {} tools", toolRegistry.getToolCount());
            
        } catch (Exception e) {
            logger.error("Error during tool discovery: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Creates a tool definition from a tool instance.
     * 
     * @param tool the tool instance
     * @return the tool definition
     */
    private ToolDefinition createDefinitionFromTool(McpTool tool) {
        return ToolDefinition.builder()
            .metadata(tool.getMetadata())
            .toolClass(tool.getClass())
            .status(ToolStatus.ENABLED)
            .build();
    }
    
    /**
     * Manually discovers and registers tools.
     * Can be called to refresh tool discovery.
     */
    public void rediscoverTools() {
        logger.info("Manually triggering tool rediscovery");
        discoverTools();
    }
    
    /**
     * Returns the number of discovered tools.
     * 
     * @return the number of tools in the registry
     */
    public int getDiscoveredToolCount() {
        return toolRegistry.getToolCount();
    }
    
    /**
     * Returns the number of enabled tools.
     * 
     * @return the number of enabled tools
     */
    public int getEnabledToolCount() {
        return toolRegistry.getEnabledToolCount();
    }
}
