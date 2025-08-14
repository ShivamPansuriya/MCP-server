package com.example.mcpserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.mcpserver.tool.api.*;
import com.example.mcpserver.tool.registry.ToolDefinition;
import com.example.mcpserver.tool.registry.ToolRegistry;
import com.example.mcpserver.tool.factory.ToolFactory;
import com.example.mcpserver.tool.discovery.ToolDiscoveryService;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;

import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Configuration class for MCP (Model Context Protocol) server setup.
 * This class configures the HttpServletStreamableServerTransportProvider and uses the tool registry system.
 */
@Configuration
public class McpServerConfig {
    private static final Logger logger = LoggerFactory.getLogger(McpServerConfig.class);

    private final ToolRegistry toolRegistry;
    private final ToolFactory toolFactory;
    private final ToolDiscoveryService toolDiscoveryService;

    public McpServerConfig(ToolRegistry toolRegistry, ToolFactory toolFactory,
            ToolDiscoveryService toolDiscoveryService) {
        this.toolRegistry = toolRegistry;
        this.toolFactory = toolFactory;
        this.toolDiscoveryService = toolDiscoveryService;
    }

    /**
     * Creates and configures the MCP server transport provider.
     * This provider handles Streamable HTTP transport with a single /mcp endpoint.
     *
     * @param objectMapper Jackson ObjectMapper for JSON serialization/deserialization
     * @return Configured HttpServletStreamableServerTransportProvider
     */
    @Bean
    public HttpServletStreamableServerTransportProvider mcpTransportProvider(ObjectMapper objectMapper) {
        return HttpServletStreamableServerTransportProvider.builder().objectMapper(objectMapper).build();
    }

    /**
     * Registers the MCP transport provider as a servlet.
     * This allows the HttpServletStreamableServerTransportProvider to handle HTTP requests.
     *
     * @param transportProvider The MCP transport provider
     * @return ServletRegistrationBean for the MCP servlet
     */
    @Bean
    public ServletRegistrationBean<HttpServletStreamableServerTransportProvider> mcpServletRegistration(
            HttpServletStreamableServerTransportProvider transportProvider) {

        ServletRegistrationBean<HttpServletStreamableServerTransportProvider> registration =
                new ServletRegistrationBean<>(transportProvider);

        // Register URL pattern for the single MCP endpoint
        registration.addUrlMappings("/mcp");
        registration.setName("mcpServlet");
        registration.setLoadOnStartup(1);

        return registration;
    }

    /**
     * Creates the MCP server instance with the configured transport provider.
     * This server will handle MCP protocol messages and route them to appropriate handlers.
     * Uses the tool registry system for dynamic tool registration.
     *
     * @param transportProvider The configured transport provider
     * @return Configured MCP server
     */
    @Bean
    public McpSyncServer mcpServer(HttpServletStreamableServerTransportProvider transportProvider) {
        // Ensure tools are discovered before creating the server
        // The ToolDiscoveryService dependency ensures @PostConstruct has run

        var builder = McpServer.sync(transportProvider).serverInfo("Example MCP Server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).prompts(true).logging()
                        .resources(true, true).build());

        // Register tools from registry
        List<ToolDefinition> enabledTools = toolRegistry.getEnabledTools();
        logger.info("Registering {} enabled tools from registry", enabledTools.size());

        for (ToolDefinition toolDef : enabledTools) {
            McpSchema.Tool mcpTool = McpSchema.Tool.builder()
                    .name(toolDef.getMetadata().getName())
                    .description(toolDef.getMetadata().getDescription())
                    .inputSchema(toolDef.getMetadata().getSchema()).build();

            builder.toolCall(mcpTool,
                    (exchange, request) -> executeToolViaRegistry(toolDef.getMetadata().getName(), request));

            logger.debug("Registered tool: {}", toolDef.getMetadata().getName());
        }

        return builder.build();
    }

    /**
     * Executes a tool via the registry system.
     * This is a temporary implementation that will be replaced by ToolExecutionEngine in Phase 3.
     *
     * @param toolName the name of the tool to execute
     * @param request  the MCP tool call request
     * @return the tool call result
     */
    private McpSchema.CallToolResult executeToolViaRegistry(String toolName,
            McpSchema.CallToolRequest request) {
        try {
            Optional<ToolDefinition> toolDef = toolRegistry.findByName(toolName);
            if (toolDef.isEmpty()) {
                logger.warn("Tool not found in registry: {}", toolName);
                return new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent("Tool not found: " + toolName)), true);
            }

            // Create tool instance using factory
            McpTool tool = toolFactory.createTool(toolDef.get());

            // Create execution context
            ToolContext context =
                    ToolContext.builder().sessionId(extractSessionId(request)).requestId(generateRequestId())
                            .build();

            // Validate arguments
            ValidationResult validation = tool.validate(request.arguments());
            if (!validation.isValid()) {
                logger.warn("Tool validation failed for {}: {}", toolName, validation.getFormattedErrors());
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(
                        "Validation failed: " + validation.getFormattedErrors())), true);
            }

            // Execute tool
            ToolResult result = tool.execute(context, request.arguments());
            toolDef.get().incrementExecutionCount();

            return new McpSchema.CallToolResult(result.getContent(), result.isError());

        } catch (Exception e) {
            logger.error("Error executing tool {}: {}", toolName, e.getMessage(), e);
            return new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent("Execution failed: " + e.getMessage())), true);
        }
    }

    /**
     * Extracts session ID from the request.
     * Temporary implementation - will be improved in later phases.
     */
    private String extractSessionId(McpSchema.CallToolRequest request) {
        // For now, return a default session ID
        // In a real implementation, this would extract from the exchange context
        return "default-session";
    }

    /**
     * Generates a unique request ID.
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}
