package com.example.mcpserver.tools.builtin;

import com.example.mcpserver.service.IncidentStorageService;
import com.example.mcpserver.tool.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DeleteIncidentToolTest {

    @Autowired
    private DeleteIncidentTool deleteIncidentTool;

    @Autowired
    private IncidentStorageService incidentStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    private ToolContext testContext;
    private String testIncidentId;

    @BeforeEach
    void setUp() {
        testContext = ToolContext.builder()
                .sessionId("test-session")
                .requestId("test-request")
                .build();

        // Create a test incident for deletion tests
        Map<String, Object> incident = incidentStorageService.createIncident(
                "Test Incident for Deletion",
                "test@example.com",
                "This incident will be used for deletion testing",
                "Medium"
        );
        testIncidentId = (String) incident.get("incident_id");
    }

    @Test
    void shouldHaveCorrectMetadata() {
        assertThat(deleteIncidentTool.getName()).isEqualTo("delete_incident");
        assertThat(deleteIncidentTool.getDescription()).contains("Safely delete an incident with user confirmation");
        
        ToolMetadata metadata = deleteIncidentTool.getMetadata();
        assertThat(metadata.getName()).isEqualTo("delete_incident");
        assertThat(metadata.getCategory()).isEqualTo("incident-management");
        assertThat(metadata.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void shouldValidateIncidentIdFormat() {
        // Valid incident ID
        Map<String, Object> validArgs = Map.of("incident_id", "INC-12345678");
        ValidationResult result = deleteIncidentTool.validate(validArgs);
        assertThat(result.isValid()).isTrue();

        // Invalid format
        Map<String, Object> invalidArgs = Map.of("incident_id", "INVALID-ID");
        result = deleteIncidentTool.validate(invalidArgs);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("must be in format INC-XXXXXXXX");
    }

    @Test
    void shouldValidateRequiredParameters() {
        // Missing incident_id
        Map<String, Object> missingArgs = Map.of();
        ValidationResult result = deleteIncidentTool.validate(missingArgs);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("Missing required parameter: 'incident_id'");

        // Null incident_id
        Map<String, Object> nullArgs = Map.of();
        nullArgs = new java.util.HashMap<>(nullArgs);
        nullArgs.put("incident_id", null);
        result = deleteIncidentTool.validate(nullArgs);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("'incident_id' cannot be null");

        // Empty incident_id
        Map<String, Object> emptyArgs = Map.of("incident_id", "");
        result = deleteIncidentTool.validate(emptyArgs);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("'incident_id' cannot be empty");
    }

    @Test
    void shouldValidateConfirmationParameter() {
        // Valid boolean confirmation
        Map<String, Object> validArgs = Map.of(
                "incident_id", "INC-12345678",
                "confirm_deletion", true
        );
        ValidationResult result = deleteIncidentTool.validate(validArgs);
        assertThat(result.isValid()).isTrue();

        // Invalid confirmation type
        Map<String, Object> invalidArgs = Map.of(
                "incident_id", "INC-12345678",
                "confirm_deletion", "yes"
        );
        result = deleteIncidentTool.validate(invalidArgs);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("'confirm_deletion' must be a boolean value");
    }

    @Test
    void shouldReturnErrorForNonExistentIncident() throws Exception {
        // Given
        Map<String, Object> args = Map.of("incident_id", "INC-NOTFOUND");

        // When
        ToolResult result = deleteIncidentTool.execute(testContext, args);

        // Then
        assertThat(result.isError()).isFalse();
        assertThat(result.getContent()).hasSize(1);
        
        String responseJson = ((McpSchema.TextContent) result.getContent().get(0)).text();
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
        
        assertThat(response.get("message")).isEqualTo("Incident not found");
        assertThat(response.get("incident_id")).isEqualTo("INC-NOTFOUND");
        assertThat(response.get("deleted")).isEqualTo(false);
    }

    @Test
    void shouldRequestConfirmationOnFirstCall() throws Exception {
        // Given
        Map<String, Object> args = Map.of("incident_id", testIncidentId);

        // When
        ToolResult result = deleteIncidentTool.execute(testContext, args);

        // Then
        assertThat(result.isError()).isFalse();
        assertThat(result.getContent()).hasSize(1);
        
        String responseJson = ((McpSchema.TextContent) result.getContent().get(0)).text();
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
        
        assertThat(response.get("message")).asString().contains("CONFIRMATION REQUIRED");
        assertThat(response.get("deleted")).isEqualTo(false);
        assertThat(response.get("warning")).asString().contains("cannot be undone");
        assertThat(response.get("next_step")).asString().contains("confirm_deletion=true");
        assertThat(response.get("incident_details")).isNotNull();
        
        // Verify incident still exists
        assertThat(incidentStorageService.incidentExists(testIncidentId)).isTrue();
    }

    @Test
    void shouldDeleteIncidentWithConfirmation() throws Exception {
        // Given
        Map<String, Object> args = Map.of(
                "incident_id", testIncidentId,
                "confirm_deletion", true
        );

        // When
        ToolResult result = deleteIncidentTool.execute(testContext, args);

        // Then
        assertThat(result.isError()).isFalse();
        assertThat(result.getContent()).hasSize(1);
        
        String responseJson = ((McpSchema.TextContent) result.getContent().get(0)).text();
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
        
        assertThat(response.get("message")).isEqualTo("Incident successfully deleted");
        assertThat(response.get("deleted")).isEqualTo(true);
        assertThat(response.get("deleted_incident")).isNotNull();
        
        // Verify incident no longer exists
        assertThat(incidentStorageService.incidentExists(testIncidentId)).isFalse();
    }

    @Test
    void shouldNotDeleteWithoutConfirmation() throws Exception {
        // Given
        Map<String, Object> args = Map.of(
                "incident_id", testIncidentId,
                "confirm_deletion", false
        );

        // When
        ToolResult result = deleteIncidentTool.execute(testContext, args);

        // Then
        assertThat(result.isError()).isFalse();
        
        String responseJson = ((McpSchema.TextContent) result.getContent().get(0)).text();
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
        
        assertThat(response.get("deleted")).isEqualTo(false);
        
        // Verify incident still exists
        assertThat(incidentStorageService.incidentExists(testIncidentId)).isTrue();
    }

    @Test
    void shouldHandleExecutionErrors() {
        // Given - arguments that will cause an error during execution
        Map<String, Object> args = Map.of("incident_id", "INC-12345678");

        // When - simulate error by passing null context
        ToolResult result = deleteIncidentTool.execute(null, args);

        // Then - the tool should handle the error gracefully and not crash
        assertThat(result.isError()).isFalse(); // Tool handles null context gracefully
        assertThat(result.getContent().get(0)).isInstanceOf(McpSchema.TextContent.class);
    }
}
