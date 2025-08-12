package com.example.mcpserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the MCP Server.
 * This Spring Boot application demonstrates how to implement an MCP server
 * using the HttpServletSseServerTransportProvider from the MCP Java SDK.
 */
@SpringBootApplication
public class McpServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(McpServerApplication.class);

    public static void main(String[] args) {
        logger.info("Starting MCP Server Application...");
        SpringApplication.run(McpServerApplication.class, args);
        logger.info("MCP Server Application started successfully");
    }

}
