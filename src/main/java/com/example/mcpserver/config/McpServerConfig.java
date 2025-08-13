package com.example.mcpserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Configuration class for MCP (Model Context Protocol) server setup.
 * This class configures the HttpServletSseServerTransportProvider and registers it as a servlet.
 */
@Configuration
public class McpServerConfig {

    /**
     * Creates and configures the MCP server transport provider.
     * This provider handles HTTP with Server-Sent Events (SSE) transport.
     *
     * @param objectMapper Jackson ObjectMapper for JSON serialization/deserialization
     * @return Configured HttpServletSseServerTransportProvider
     */
    @Bean
    public HttpServletStatelessServerTransport mcpTransportProvider(ObjectMapper objectMapper) {
        return HttpServletStatelessServerTransport.builder()
                .objectMapper(objectMapper)
                .messageEndpoint("/mcp")
                .build();
    }

    /**
     * Registers the MCP transport provider as a servlet.
     * This allows the HttpServletSseServerTransportProvider to handle HTTP requests.
     *
     * @param transportProvider The MCP transport provider
     * @return ServletRegistrationBean for the MCP servlet
     */
    @Bean
    public ServletRegistrationBean<HttpServletStatelessServerTransport> mcpServletRegistration(
            HttpServletStatelessServerTransport transportProvider) {

        ServletRegistrationBean<HttpServletStatelessServerTransport> registration =
                new ServletRegistrationBean<>(transportProvider);

        // Register URL patterns that the servlet should handle
        registration.addUrlMappings("/sse", "/mcp/message");
        registration.setName("mcpServlet");
        registration.setLoadOnStartup(1);

        return registration;
    }

    /**
     * Creates the MCP server instance with the configured transport provider.
     * This server will handle MCP protocol messages and route them to appropriate handlers.
     *
     * @param transportProvider The configured transport provider
     * @return Configured MCP server
     */
    @Bean
    public McpStatelessSyncServer mcpServer(HttpServletStatelessServerTransport transportProvider) {
        // JSON schema for tools
        String emptyJsonSchema = """
                {
                    "$schema": "http://json-schema.org/draft-07/schema#",
                    "type": "object",
                    "properties": {}
                }
                """;

        String timeToolSchema = """
                {
                    "$schema": "http://json-schema.org/draft-07/schema#",
                    "type": "object",
                    "properties": {
                        "format": {
                            "type": "string",
                            "description": "Optional time format: 'iso' for ISO format, 'readable' for human-readable format",
                            "enum": ["iso", "readable"]
                        }
                    },
                    "required": []
                }
                """;

        String echoToolSchema = """
                {
                    "$schema": "http://json-schema.org/draft-07/schema#",
                    "type": "object",
                    "properties": {
                        "text": {
                            "type": "string",
                            "description": "Text to echo back"
                        }
                    },
                    "required": ["text"]
                }
                """;

        // Define the get_current_time tool
        var getCurrentTimeTool = new McpSchema.Tool(
                "get_current_time",
                "Returns the current date and time",
                timeToolSchema
        );

        // Define the echo tool
        var echoTool = new McpSchema.Tool(
                "echo",
                "Echoes back the provided text",
                echoToolSchema
        );

        return McpServer.sync(transportProvider)
                .serverInfo("Example MCP Server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .toolCall(getCurrentTimeTool, (exchange, request) -> {
                    Map<String, Object> arguments = request.arguments();
                    String format = (String) arguments.getOrDefault("format", "readable");
                    LocalDateTime now = LocalDateTime.now();
                    String timeString;

                    if ("iso".equals(format)) {
                        timeString = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } else {
                        timeString = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }

                    return new McpSchema.CallToolResult(
                            List.of(new McpSchema.TextContent("Current time: " + timeString)),
                            false
                    );
                })
                .toolCall(echoTool, (exchange, request) -> {
                    Map<String, Object> arguments = request.arguments();
                    String text = (String) arguments.get("text");

                    if (text == null) {
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent("Error: 'text' parameter is required")),
                                true
                        );
                    }

                    return new McpSchema.CallToolResult(
                            List.of(new McpSchema.TextContent("Echo: " + text)),
                            false
                    );
                })
                .build();
    }
}
