package com.example.mcpserver.tools.builtin;

import com.example.mcpserver.service.IncidentStorageService;
import com.example.mcpserver.tool.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tool for creating new incident requests.
 * IMPORTANT: Before using this tool, first call the 'get_fields_schema' tool with model="request"
 * to fetch the complete field schema and understand all available fields, their types,
 * validation rules, and required status.
 * Creates incidents with proper field validation and unique ID generation.
 */
@Component
public class CreateIncidentTool implements McpTool {

    private static final Logger logger = LoggerFactory.getLogger(CreateIncidentTool.class);

    private final IncidentStorageService incidentStorageService;
    private final ObjectMapper objectMapper;
    
    private static final String SCHEMA = """
        {
            "$schema": "http://json-schema.org/draft-07/schema#",
            "type": "object",
            "description": "Create a new incident request. IMPORTANT: First call 'get_fields_schema' tool with model='request' to get complete field schema and use exact field names from that response.",
            "properties": {
                "incident_data": {
                    "type": "object",
                    "description": "Object containing incident field data with field names as keys and field values as values. Use exact field names from get_fields_schema tool response. make sure that all the required fields are present in the object.",
                    "additionalProperties": true
                }
            },
            "required": ["incident_data"],
            "examples": [
                {
                    "incident_data": {
                        "Subject": "Database connectivity issue",
                        "Requester": "john.doe@company.com",
                        "Description": "Users unable to connect to main database server",
                        "Priority": "High",
                        "Department": "IT"
                    }
                },
                {
                    "incident_data": {
                        "Subject": "Printer not working",
                        "Requester": "jane.smith@company.com",
                        "Description": "Office printer on 2nd floor is not responding",
                        "Priority": "Medium",
                        "Department": "Administration",
                        "Cc Emails": ["admin@company.com"]
                    }
                }
            ]
        }
        """;
    
    @Autowired
    public CreateIncidentTool(IncidentStorageService incidentStorageService, ObjectMapper objectMapper) {
        this.incidentStorageService = incidentStorageService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "create_incident";
    }
    
    @Override
    public String getDescription() {
        return "Create a new incident request. IMPORTANT: First call 'get_fields_schema' tool with model='request' to fetch complete field details including field names, types, validation rules, and required status. Then use this tool with the exact field names and properly formatted values from the get_fields_schema response.";
    }
    
    @Override
    public String getSchema() {
        return SCHEMA;
    }
    
    @Override
    public ToolMetadata getMetadata() {
        return ToolMetadata.builder()
                .name(getName())
                .description(getDescription())
                .version("1.0.0")
                .category("request-management")
                .schema(getSchema())
                .build();
    }
    
    @Override
    public ValidationResult validate(Map<String, Object> arguments) {
        logger.debug("Validating create_incident arguments: {}", arguments.keySet());

        if (arguments == null || arguments.isEmpty()) {
            logger.warn("Validation failed: no arguments provided");
            return ValidationResult.failure("Arguments are required. First call 'get_fields_schema' tool with model='request' to get field schema.");
        }

        // Check for incident_data parameter
        if (!arguments.containsKey("incident_data") || arguments.get("incident_data") == null) {
            logger.warn("Validation failed: missing 'incident_data' parameter");
            return ValidationResult.failure("'incident_data' parameter is required. This should contain all field data from get_fields_schema response.");
        }

        Object incidentDataObj = arguments.get("incident_data");
        if (!(incidentDataObj instanceof Map)) {
            logger.warn("Validation failed: 'incident_data' must be an object");
            return ValidationResult.failure("'incident_data' must be an object containing field names and values.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> incidentData = (Map<String, Object>) incidentDataObj;

        if (incidentData.isEmpty()) {
            logger.warn("Validation failed: 'incident_data' is empty");
            return ValidationResult.failure("'incident_data' cannot be empty. Include field data from get_fields_schema response.");
        }

        // Validate required fields
        ValidationResult requiredFieldsValidation = validateRequiredFields(incidentData);
        if (!requiredFieldsValidation.isValid()) {
            return requiredFieldsValidation;
        }

        logger.debug("Validation successful for create_incident with {} fields in incident_data", incidentData.size());
        return ValidationResult.success();
    }

    /**
     * Validates that all required fields are present in the incident data.
     *
     * @param incidentData the incident data to validate
     * @return ValidationResult indicating success or failure with missing required fields
     */
    private ValidationResult validateRequiredFields(Map<String, Object> incidentData) {
        logger.debug("Validating required fields for incident creation");

        // Get required fields from FieldConstants
        List<String> requiredFields = FieldConstants.UPDATABLE_FIELDS.stream()
                .filter(field -> Boolean.TRUE.equals(field.get("required")))
                .map(field -> (String) field.get("name"))
                .collect(Collectors.toList());

        logger.debug("Required fields for request model: {}", requiredFields);

        // Check for missing required fields
        List<String> missingFields = requiredFields.stream()
                .filter(fieldName -> !incidentData.containsKey(fieldName) ||
                                   incidentData.get(fieldName) == null ||
                                   (incidentData.get(fieldName) instanceof String &&
                                    ((String) incidentData.get(fieldName)).trim().isEmpty()))
                .collect(Collectors.toList());

        if (!missingFields.isEmpty()) {
            String errorMessage = String.format(
                "Missing required fields: %s. Please provide values for all required fields. " +
                "Call 'get_fields_schema' tool with model='request' to see all required fields.",
                String.join(", ", missingFields)
            );
            logger.warn("Validation failed: {}", errorMessage);
            return ValidationResult.failure(errorMessage);
        }

        logger.debug("All required fields are present");
        return ValidationResult.success();
    }

    @Override
    public Mono<ToolResult> execute(McpAsyncServerExchange exchange, ToolContext context, Map<String, Object> arguments) {
        logger.info("Executing create_incident tool for session: {}", context.getSessionId());

        return Mono.fromCallable(() -> {
            // Extract incident_data from arguments
            @SuppressWarnings("unchecked")
            Map<String, Object> incidentData = (Map<String, Object>) arguments.get("incident_data");

            logger.debug("Creating incident with field data: {}", incidentData.keySet());

            // Create incident with the extracted field data
            Map<String, Object> incident = incidentStorageService.createIncidentWithFields(incidentData);
            String incidentId = (String) incident.get("incident_id");

            logger.info("Successfully created incident with ID: {}", incidentId);

            // Format response with all created incident data
            String responseMessage = "Incident created successfully with provided field data";
            String responseJson = objectMapper.writeValueAsString(Map.of(
                "message", responseMessage,
                "incident_id", incidentId,
                "incident", incident
            ));

            logger.debug("Returning successful response for incident: {}", incidentId);
            return ToolResult.success(
                List.of(new McpSchema.TextContent(responseJson))
            );
        })
        .doOnError(error -> logger.error("Failed to create incident: {}", error.getMessage(), error))
        .onErrorResume(error -> Mono.just(ToolResult.error("Failed to create incident: " + error.getMessage())));
    }
}
