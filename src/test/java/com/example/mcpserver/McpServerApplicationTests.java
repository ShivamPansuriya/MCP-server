package com.example.mcpserver;

import io.modelcontextprotocol.server.McpSyncServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private McpSyncServer mcpServer;

    @Test
    void contextLoads() {
        // Verify that the Spring context loads successfully
        assertThat(mcpServer).isNotNull();
    }

    @Test
    void mcpServerHealthCheck() {
        // Test the health endpoint
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/mcp/health", Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    void mcpServerInfo() {
        // Test the info endpoint
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/mcp/info", Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("name")).isEqualTo("Example MCP Server");
        assertThat(response.getBody().get("version")).isEqualTo("1.0.0");
    }

    @Test
    void mcpServerStatus() {
        // Test the status endpoint
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/mcp/status", Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("running")).isEqualTo(true);
        assertThat(response.getBody().get("transport")).isEqualTo("HttpServletSseServerTransportProvider");
    }

    @Test
    void mcpServerIsRunning() {
        // Verify that the MCP server is configured
        assertThat(mcpServer).isNotNull();
    }
}
