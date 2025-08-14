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

        // Then
        assertThat(toolRegistry.isRegistered("get_current_time")).isTrue();
        assertThat(toolRegistry.isRegistered("echo")).isTrue();
        assertThat(discoveryService.getDiscoveredToolCount()).isEqualTo(2);
        assertThat(discoveryService.getEnabledToolCount()).isEqualTo(2);
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
