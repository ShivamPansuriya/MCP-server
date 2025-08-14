package com.example.mcpserver.tool.registry;

import com.example.mcpserver.tool.api.ToolMetadata;
import com.example.mcpserver.tools.builtin.GetCurrentTimeTool;
import com.example.mcpserver.tools.builtin.EchoTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolRegistryTest {

    @Mock
    private ToolRegistryListener listener;

    private ToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        toolRegistry = new ToolRegistry();
        toolRegistry.addListener(listener);
    }

    @Test
    void shouldRegisterTool() {
        // Given
        ToolDefinition definition = createTestToolDefinition("test-tool", "utility");

        // When
        toolRegistry.register(definition);

        // Then
        assertThat(toolRegistry.isRegistered("test-tool")).isTrue();
        assertThat(toolRegistry.getToolCount()).isEqualTo(1);
        verify(listener).onToolRegistered(definition);
    }

    @Test
    void shouldFindToolByName() {
        // Given
        ToolDefinition definition = createTestToolDefinition("test-tool", "utility");
        toolRegistry.register(definition);

        // When
        Optional<ToolDefinition> found = toolRegistry.findByName("test-tool");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(definition);
    }

    @Test
    void shouldReturnEmptyForNonExistentTool() {
        // When
        Optional<ToolDefinition> found = toolRegistry.findByName("non-existent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldGetAllTools() {
        // Given
        ToolDefinition tool1 = createTestToolDefinition("tool1", "utility");
        ToolDefinition tool2 = createTestToolDefinition("tool2", "data");
        toolRegistry.register(tool1);
        toolRegistry.register(tool2);

        // When
        List<ToolDefinition> allTools = toolRegistry.getAllTools();

        // Then
        assertThat(allTools).hasSize(2);
        assertThat(allTools).containsExactlyInAnyOrder(tool1, tool2);
    }

    @Test
    void shouldGetEnabledTools() {
        // Given
        ToolDefinition enabledTool = createTestToolDefinition("enabled", "utility");
        ToolDefinition disabledTool = createTestToolDefinition("disabled", "utility");
        disabledTool.setStatus(ToolStatus.DISABLED);
        
        toolRegistry.register(enabledTool);
        toolRegistry.register(disabledTool);

        // When
        List<ToolDefinition> enabledTools = toolRegistry.getEnabledTools();

        // Then
        assertThat(enabledTools).hasSize(1);
        assertThat(enabledTools.get(0)).isEqualTo(enabledTool);
    }

    @Test
    void shouldFindToolsByCategory() {
        // Given
        ToolDefinition utilityTool1 = createTestToolDefinition("util1", "utility");
        ToolDefinition utilityTool2 = createTestToolDefinition("util2", "utility");
        ToolDefinition dataTool = createTestToolDefinition("data1", "data");
        
        toolRegistry.register(utilityTool1);
        toolRegistry.register(utilityTool2);
        toolRegistry.register(dataTool);

        // When
        List<ToolDefinition> utilityTools = toolRegistry.findByCategory("utility");
        List<ToolDefinition> dataTools = toolRegistry.findByCategory("data");

        // Then
        assertThat(utilityTools).hasSize(2);
        assertThat(utilityTools).containsExactlyInAnyOrder(utilityTool1, utilityTool2);
        assertThat(dataTools).hasSize(1);
        assertThat(dataTools).containsExactly(dataTool);
    }

    @Test
    void shouldGetCategories() {
        // Given
        toolRegistry.register(createTestToolDefinition("tool1", "utility"));
        toolRegistry.register(createTestToolDefinition("tool2", "data"));
        toolRegistry.register(createTestToolDefinition("tool3", "utility"));

        // When
        Set<String> categories = toolRegistry.getCategories();

        // Then
        assertThat(categories).containsExactlyInAnyOrder("utility", "data");
    }

    @Test
    void shouldUnregisterTool() {
        // Given
        ToolDefinition definition = createTestToolDefinition("test-tool", "utility");
        toolRegistry.register(definition);

        // When
        boolean unregistered = toolRegistry.unregister("test-tool");

        // Then
        assertThat(unregistered).isTrue();
        assertThat(toolRegistry.isRegistered("test-tool")).isFalse();
        assertThat(toolRegistry.getToolCount()).isEqualTo(0);
        verify(listener).onToolUnregistered(definition);
    }

    @Test
    void shouldReturnFalseWhenUnregisteringNonExistentTool() {
        // When
        boolean unregistered = toolRegistry.unregister("non-existent");

        // Then
        assertThat(unregistered).isFalse();
    }

    @Test
    void shouldRejectNullToolDefinition() {
        // When/Then
        assertThatThrownBy(() -> toolRegistry.register(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Tool definition cannot be null");
    }

    @Test
    void shouldReplaceExistingTool() {
        // Given
        ToolDefinition original = createTestToolDefinition("test-tool", "utility");
        ToolDefinition replacement = createTestToolDefinition("test-tool", "data");
        toolRegistry.register(original);

        // When
        toolRegistry.register(replacement);

        // Then
        assertThat(toolRegistry.getToolCount()).isEqualTo(1);
        Optional<ToolDefinition> found = toolRegistry.findByName("test-tool");
        assertThat(found).isPresent();
        assertThat(found.get().getMetadata().getCategory()).isEqualTo("data");
    }

    @Test
    void shouldClearRegistry() {
        // Given
        toolRegistry.register(createTestToolDefinition("tool1", "utility"));
        toolRegistry.register(createTestToolDefinition("tool2", "data"));

        // When
        toolRegistry.clear();

        // Then
        assertThat(toolRegistry.getToolCount()).isEqualTo(0);
        assertThat(toolRegistry.getCategories()).isEmpty();
        verify(listener).onRegistryCleared();
    }

    @Test
    void shouldUpdateLastAccessTime() {
        // Given
        ToolDefinition definition = createTestToolDefinition("test-tool", "utility");
        toolRegistry.register(definition);
        long initialAccessTime = definition.getLastAccessTime().toEpochMilli();

        // When
        toolRegistry.findByName("test-tool");

        // Then
        long updatedAccessTime = definition.getLastAccessTime().toEpochMilli();
        assertThat(updatedAccessTime).isGreaterThanOrEqualTo(initialAccessTime);
    }

    private ToolDefinition createTestToolDefinition(String name, String category) {
        ToolMetadata metadata = ToolMetadata.builder()
            .name(name)
            .description("Test tool: " + name)
            .category(category)
            .version("1.0.0")
            .build();

        return ToolDefinition.builder()
            .metadata(metadata)
            .toolClass(GetCurrentTimeTool.class)
            .status(ToolStatus.ENABLED)
            .build();
    }
}
