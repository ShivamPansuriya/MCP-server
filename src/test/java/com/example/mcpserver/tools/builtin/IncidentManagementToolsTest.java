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
class IncidentManagementToolsTest {

    @Autowired
    private CreateIncidentTool createIncidentTool;

    @Autowired
    private GetUpdatableFieldsTool getUpdatableFieldsTool;

    @Autowired
    private UpdateIncidentTool updateIncidentTool;

    @Autowired
    private IncidentStorageService incidentStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    private ToolContext testContext;

    @BeforeEach
    void setUp() {
        testContext = ToolContext.builder()
                .sessionId("test-session")
                .requestId("test-request")
                .build();
    }

    // CreateIncidentTool Tests
    @Test
    void createIncident_shouldValidateRequiredFields() {
        // Test missing title
        Map<String, Object> argsWithoutTitle = Map.of("requester", "test@example.com");
        ValidationResult result = createIncidentTool.validate(argsWithoutTitle);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("'title' parameter is required");

        // Test missing requester
        Map<String, Object> argsWithoutRequester = Map.of("title", "Test Incident");
        result = createIncidentTool.validate(argsWithoutRequester);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("'requester' parameter is required");
    }

    @Test
    void createIncident_shouldValidateFieldLengths() {
        // Test title too long
        String longTitle = "a".repeat(201);
        Map<String, Object> args = Map.of(
            "title", longTitle,
            "requester", "test@example.com"
        );
        ValidationResult result = createIncidentTool.validate(args);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("'title' cannot exceed 200 characters");

        // Test requester too long
        String longRequester = "a".repeat(101);
        args = Map.of(
            "title", "Test Incident",
            "requester", longRequester
        );
        result = createIncidentTool.validate(args);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("'requester' cannot exceed 100 characters");

        // Test description too long
        String longDescription = "a".repeat(2001);
        args = Map.of(
            "title", "Test Incident",
            "requester", "test@example.com",
            "description", longDescription
        );
        result = createIncidentTool.validate(args);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("'description' cannot exceed 2000 characters");
    }

    @Test
    void createIncident_shouldValidatePriority() {
        Map<String, Object> args = Map.of(
            "title", "Test Incident",
            "requester", "test@example.com",
            "priority", "Invalid"
        );
        ValidationResult result = createIncidentTool.validate(args);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("'priority' must be one of: Low, Medium, High, Critical");
    }

    @Test
    void createIncident_shouldCreateValidIncident() throws Exception {
        Map<String, Object> args = Map.of(
            "title", "Test Incident",
            "requester", "test@example.com",
            "description", "Test description",
            "priority", "High"
        );

        ValidationResult validation = createIncidentTool.validate(args);
        assertThat(validation.isValid()).isTrue();

        ToolResult result = createIncidentTool.execute(testContext, args);
        assertThat(result.isError()).isFalse();
        assertThat(result.getContent()).hasSize(1);

        String responseJson = ((McpSchema.TextContent) result.getContent().get(0)).text();
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
        
        assertThat(response.get("message")).isEqualTo("Incident created successfully");
        assertThat(response.get("incident_id")).asString().matches("INC-[A-F0-9]{8}");
        assertThat(response.get("title")).isEqualTo("Test Incident");
        assertThat(response.get("requester")).isEqualTo("test@example.com");
        assertThat(response.get("description")).isEqualTo("Test description");
        assertThat(response.get("priority")).isEqualTo("High");
        assertThat(response.get("status")).isEqualTo("Open");
        assertThat(response.get("created_at")).isNotNull();
    }

    // GetUpdatableFieldsTool Tests
    @Test
    void getUpdatableFields_shouldReturnFieldMetadata() throws Exception {
        ValidationResult validation = getUpdatableFieldsTool.validate(Map.of());
        assertThat(validation.isValid()).isTrue();

        ToolResult result = getUpdatableFieldsTool.execute(testContext, Map.of());
        assertThat(result.isError()).isFalse();
        assertThat(result.getContent()).hasSize(1);

        String responseJson = ((McpSchema.TextContent) result.getContent().get(0)).text();
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
        
        assertThat(response).containsKey("updatable_fields");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, String>> fields = (java.util.List<Map<String, String>>) response.get("updatable_fields");
        
        assertThat(fields).hasSize(4);
        assertThat(fields).extracting(field -> field.get("field_name"))
            .containsExactlyInAnyOrder("title", "description", "priority", "status");
    }

    // UpdateIncidentTool Tests
    @Test
    void updateIncident_shouldValidateRequiredFields() {
        // Test missing incident_id
        Map<String, Object> argsWithoutId = Map.of("updates", Map.of("title", "New Title"));
        ValidationResult result = updateIncidentTool.validate(argsWithoutId);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("'incident_id' parameter is required");

        // Test missing updates
        Map<String, Object> argsWithoutUpdates = Map.of("incident_id", "INC-12345678");
        result = updateIncidentTool.validate(argsWithoutUpdates);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("'updates' parameter is required");
    }

    @Test
    void updateIncident_shouldValidateIncidentIdFormat() {
        Map<String, Object> args = Map.of(
            "incident_id", "INVALID-ID",
            "updates", Map.of("title", "New Title")
        );
        ValidationResult result = updateIncidentTool.validate(args);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("'incident_id' must be in format INC-XXXXXXXX where X is a hex digit");
    }

    @Test
    void updateIncident_shouldValidateUpdateFields() {
        Map<String, Object> args = Map.of(
            "incident_id", "INC-12345678",
            "updates", Map.of("invalid_field", "value")
        );
        ValidationResult result = updateIncidentTool.validate(args);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("Invalid field 'invalid_field'");
    }

    @Test
    void updateIncident_shouldValidateFieldValues() {
        // Test invalid priority
        Map<String, Object> args = Map.of(
            "incident_id", "INC-12345678",
            "updates", Map.of("priority", "Invalid")
        );
        ValidationResult result = updateIncidentTool.validate(args);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("'priority' must be one of: Low, Medium, High, Critical");

        // Test invalid status
        args = Map.of(
            "incident_id", "INC-12345678",
            "updates", Map.of("status", "Invalid")
        );
        result = updateIncidentTool.validate(args);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFormattedErrors()).contains("'status' must be one of: Open, In Progress, Resolved, Closed");
    }

    @Test
    void updateIncident_shouldHandleNonExistentIncident() throws Exception {
        Map<String, Object> args = Map.of(
            "incident_id", "INC-12345678",
            "updates", Map.of("title", "New Title")
        );

        ValidationResult validation = updateIncidentTool.validate(args);
        assertThat(validation.isValid()).isTrue();

        ToolResult result = updateIncidentTool.execute(testContext, args);
        assertThat(result.isError()).isFalse();

        String responseJson = ((McpSchema.TextContent) result.getContent().get(0)).text();
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
        
        assertThat(response.get("message")).isEqualTo("Incident not found");
        assertThat(response.get("updated_incident")).isEqualTo(Map.of());
    }

    @Test
    void updateIncident_shouldUpdateExistingIncident() throws Exception {
        // First create an incident
        Map<String, Object> createdIncident = incidentStorageService.createIncident("Original Title", "test@example.com", "Original description", "Low");
        String incidentId = (String) createdIncident.get("incident_id");

        // Now update it
        Map<String, Object> args = Map.of(
            "incident_id", incidentId,
            "updates", Map.of(
                "title", "Updated Title",
                "priority", "High",
                "status", "In Progress"
            )
        );

        ValidationResult validation = updateIncidentTool.validate(args);
        assertThat(validation.isValid()).isTrue();

        ToolResult result = updateIncidentTool.execute(testContext, args);
        assertThat(result.isError()).isFalse();

        String responseJson = ((McpSchema.TextContent) result.getContent().get(0)).text();
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
        
        assertThat(response.get("message")).isEqualTo("Incident updated successfully");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> updatedIncident = (Map<String, Object>) response.get("updated_incident");
        assertThat(updatedIncident.get("title")).isEqualTo("Updated Title");
        assertThat(updatedIncident.get("priority")).isEqualTo("High");
        assertThat(updatedIncident.get("status")).isEqualTo("In Progress");
        assertThat(updatedIncident.get("requester")).isEqualTo("test@example.com"); // Should remain unchanged
    }
}
