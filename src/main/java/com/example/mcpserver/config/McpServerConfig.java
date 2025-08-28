package com.example.mcpserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.mcpserver.tool.api.*;
import com.example.mcpserver.tool.registry.ToolDefinition;
import com.example.mcpserver.tool.registry.ToolRegistry;
import com.example.mcpserver.tool.factory.ToolFactory;
import com.example.mcpserver.tool.discovery.ToolDiscoveryService;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import reactor.core.publisher.Mono;

import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
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
     * Creates the MCP async server instance with the configured transport provider.
     * This server will handle MCP protocol messages and route them to appropriate handlers.
     * Uses the tool registry system for dynamic tool registration.
     *
     * @param transportProvider The configured transport provider
     * @return Configured MCP async server
     */
    @Bean
    public McpAsyncServer mcpServer(HttpServletStreamableServerTransportProvider transportProvider) {
        // Ensure tools are discovered before creating the server
        // The ToolDiscoveryService dependency ensures @PostConstruct has run

        var builder = McpServer.async(transportProvider).serverInfo("Example MCP Server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).prompts(true).logging()
                        .resources(true, true).build());

        // Register tools from registry
        List<ToolDefinition> enabledTools = toolRegistry.getEnabledTools();
        logger.info("Registering {} enabled tools from registry", enabledTools.size());

        for (ToolDefinition toolDef : enabledTools) {
            McpSchema.Tool mcpTool = McpSchema.Tool.builder().name(toolDef.getMetadata().getName())
                    .description(toolDef.getMetadata().getDescription())
                    .inputSchema(toolDef.getMetadata().getSchema()).build();

            builder.toolCall(mcpTool,
                    (exchange, request) -> executeToolViaRegistry(exchange, toolDef.getMetadata().getName(),
                            request));

            logger.debug("Registered tool: {}", toolDef.getMetadata().getName());
        }

        // TODO: Add resource handlers when MCP SDK API is clarified
        // For now, field schema will be available through tools
        McpSchema.Resource resource = McpSchema.Resource.builder().name("field://schema")
                .description("Field schema data").build();
        builder.resources(new McpServerFeatures.AsyncResourceSpecification(resource, (exchange, request) -> {
            return Mono.empty();
        }));

        builder.requestTimeout(Duration.ofSeconds(40));
        return builder.build();
    }

    /**
     * Executes a tool via the registry system asynchronously.
     * This implementation uses reactive patterns for non-blocking tool execution.
     *
     * @param exchange the MCP async server exchange for client interaction
     * @param toolName the name of the tool to execute
     * @param request  the MCP tool call request
     * @return the tool call result wrapped in a Mono
     */
    private Mono<McpSchema.CallToolResult> executeToolViaRegistry(McpAsyncServerExchange exchange,
            String toolName, McpSchema.CallToolRequest request) {
        return Mono.defer(() -> {
                    // Find tool definition
                    ToolDefinition toolDef = toolRegistry.findByName(toolName).orElseThrow(() -> {
                        logger.warn("Tool not found in registry: {}", toolName);
                        return new IllegalArgumentException("Tool not found: " + toolName);
                    });

                    // Create tool instance using factory
                    McpTool tool = toolFactory.createTool(toolDef);

                    // Create execution context
                    ToolContext context =
                            ToolContext.builder().sessionId(extractSessionId(request)).requestId(generateRequestId())
                                    .build();

                    // Validate arguments
                    ValidationResult validation = tool.validate(request.arguments());
                    if (!validation.isValid()) {
                        logger.warn("Tool validation failed for {}: {}", toolName, validation.getFormattedErrors());
                        throw new IllegalArgumentException("Validation failed: " + validation.getFormattedErrors());
                    }

                    // Execute tool asynchronously using the new async interface
                    return tool.execute(exchange, context, request.arguments())
                            .doOnSuccess(result -> toolDef.incrementExecutionCount())
                            .map(result -> new McpSchema.CallToolResult(result.getContent(), result.isError()));
                }).doOnError(
                        error -> logger.error("Error executing tool {}: {}", toolName, error.getMessage(), error))
                .onErrorResume(error -> {
                    String errorMessage = error instanceof IllegalArgumentException ?
                            error.getMessage() :
                            "Execution failed: " + error.getMessage();
                    return Mono.just(
                            new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(errorMessage)),
                                    true));
                });
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
