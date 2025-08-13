package com.example.mcpserver.controller;

import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for managing and monitoring the MCP server.
 * Provides endpoints for server status, health checks, and administrative operations.
 */
@RestController
public class McpServerController {

    private final McpStatelessSyncServer mcpServer;
    private final HttpServletStatelessServerTransport transportProvider;

    @Autowired
    public McpServerController(McpStatelessSyncServer mcpServer, HttpServletStatelessServerTransport transportProvider) {
        this.mcpServer = mcpServer;
        this.transportProvider = transportProvider;
    }

    /**
     * Health check endpoint to verify the MCP server is running.
     *
     * @return Server status information
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = Map.of(
                "status", "UP",
                "server", "MCP Server",
                "version", "1.0.0",
                "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(status);
    }

    /**
     * Get server information and configuration.
     * 
     * @return Server information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = Map.of(
                "name", "Example MCP Server",
                "version", "1.0.0",
                "description", "A sample MCP server implementation using HttpServletSseServerTransportProvider",
                "endpoints", Map.of(
                        "sse", "/sse",
                        "message", "/mcp/message"
                ),
                "features", Map.of(
                        "tools", true,
                        "resources", true,
                        "notifications", true
                )
        );
        
        return ResponseEntity.ok(info);
    }

    /**
     * Send a test notification to all connected MCP clients.
     *
     * @param request The notification request containing method and params
     * @return Response indicating success or failure
     */
//    @PostMapping("/notify")
//    public ResponseEntity<Map<String, String>> sendNotification(@RequestBody NotificationRequest request) {
//        try {
//            transportProvider.notifyClients(request.getMethod(), request.getParams()).block();
//            return ResponseEntity.ok(Map.of(
//                    "status", "success",
//                    "message", "Notification sent successfully"
//            ));
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().body(Map.of(
//                    "status", "error",
//                    "message", "Failed to send notification: " + e.getMessage()
//            ));
//        }
//    }

    /**
     * Get the current server status and statistics.
     *
     * @return Server status and statistics
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> status = Map.of(
                "running", true,
                "uptime", System.currentTimeMillis(), // In a real implementation, track actual uptime
                "transport", "HttpServletSseServerTransportProvider",
                "protocol", "MCP 2024-11-05"
        );

        return ResponseEntity.ok(status);
    }

    /**
     * Request object for sending notifications.
     */
    public static class NotificationRequest {
        private String method;
        private Object params;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public Object getParams() {
            return params;
        }

        public void setParams(Object params) {
            this.params = params;
        }
    }
}
