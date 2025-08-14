package com.example.mcpserver.tool.discovery;

import com.example.mcpserver.tool.registry.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ToolDiscoveryServiceTest {

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private ToolDiscoveryService discoveryService;

    @Test
    void shouldDiscoverBuiltinTools() {
        // When - discovery happens automatically via @PostConstruct
        // But we can verify the results

        // Then - verify original tools
        assertThat(toolRegistry.isRegistered("get_current_time")).isTrue();
        assertThat(toolRegistry.isRegistered("echo")).isTrue();

        // Verify new incident management tools
        assertThat(toolRegistry.isRegistered("create_incident")).isTrue();
        assertThat(toolRegistry.isRegistered("get_updatable_fields")).isTrue();
        assertThat(toolRegistry.isRegistered("update_incident")).isTrue();
        assertThat(toolRegistry.isRegistered("delete_incident")).isTrue();

        // Verify total count (2 original + 4 incident management = 6)
        assertThat(discoveryService.getDiscoveredToolCount()).isEqualTo(6);
        assertThat(discoveryService.getEnabledToolCount()).isEqualTo(6);
    }

    @Test
    void shouldRediscoverTools() {
        // Given
        int initialCount = discoveryService.getDiscoveredToolCount();

        // When
        discoveryService.rediscoverTools();

        // Then
        assertThat(discoveryService.getDiscoveredToolCount()).isGreaterThanOrEqualTo(initialCount);
    }
}
