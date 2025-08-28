package com.example.mcpserver.tools.builtin;

import java.util.List;
import java.util.Map;

public class FieldConstants {
    private FieldConstants() {
    }

    public static final List<Map<String, Object>> UPDATABLE_FIELDS = List.of(
            Map.of(
                    "name", "Fill From Template",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Fill From Template"
            ),

            Map.of(
                    "name", "Requester",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", true,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Requester of the incident"
            ),

            Map.of(
                    "name", "Cc Emails",
                    "type", "array",
                    "items", Map.of("type", "string"),
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", true,
                    "caseExact", false,
                    "description", "CC email addresses"
            ),

            Map.of(
                    "name", "Subject",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", true,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Subject of the incident",
                    "placeholder", "Describe problem in short"
            ),

            Map.of(
                    "name", "Description",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Description of the incident"
            ),

            Map.of(
                    "name", "Status",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Status of the incident",
                    "canonicalValues", List.of("Open", "In Progress", "Resolved", "Closed")
            ),

            Map.of(
                    "name", "Priority",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Priority level of the incident",
                    "canonicalValues", List.of("Low", "Medium", "High", "Critical")
            ),

            Map.of(
                    "name", "Urgency",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Urgency level of the incident",
                    "canonicalValues", List.of("Low", "On User", "High", "Organisation")
            ),

            Map.of(
                    "name", "Impact",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Impact level of the incident"
            ),

            Map.of(
                    "name", "Category",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Category of the incident"
            ),

            Map.of(
                    "name", "Technician Group",
                    "type", "string",
                    "mutability", "readOnly",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Technician group assigned to the incident"
            ),

            Map.of(
                    "name", "Assignee",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Technician assigned to the incident"
            ),

            Map.of(
                    "name", "Department",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Department of the requester"
            ),

            Map.of(
                    "name", "Location",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Location related to the incident"
            ),

            Map.of(
                    "name", "Tags",
                    "type", "array",
                    "items", Map.of("type", "string"),
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", true,
                    "caseExact", false,
                    "description", "Tags associated with the incident"
            ),

            Map.of(
                    "name", "Attachment",
                    "type", "array",
                    "items", Map.of("type", "string"),
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", true,
                    "caseExact", false,
                    "description", "File attachments for the incident"
            ),

            Map.of(
                    "name", "Vendor",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Vendor associated with the incident"
            ),

            Map.of(
                    "name", "Transition Model",
                    "type", "string",
                    "mutability", "readOnly",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Transition model for the incident"
            ),

            Map.of(
                    "name", "New Dropdown",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Custom dropdown field",
                    "canonicalValues", List.of()
            ),

            Map.of(
                    "name", "New Text Area Test",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Custom text area field for testing"
            ),

            Map.of(
                    "name", "Categories",
                    "type", "string",
                    "mutability", "readOnly",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Custom categories field",
                    "canonicalValues", List.of("Website", "HRMS", "DS")
            ),

            Map.of(
                    "name", "Issue Repeated earlier",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Indicates if the issue was repeated earlier",
                    "canonicalValues", List.of("Yes", "NO", "Dont Konow")
            ),

            Map.of(
                    "name", "test",
                    "type", "string",
                    "mutability", "readOnly",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Test field"
            ),

            Map.of(
                    "name", "testfield",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Test field for validation"
            ),

            Map.of(
                    "name", "Tech GRP",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Technical group field"
            ),

            Map.of(
                    "name", "New Dependent",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Dependent field with hierarchical values",
                    "canonicalValues", List.of("ee", "a", "b")
            ),

            Map.of(
                    "name", "My List",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Custom list field",
                    "canonicalValues", List.of("List1")
            ),

            Map.of(
                    "name", "Testing 2",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", true,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Testing field 2"
            ),

            Map.of(
                    "name", "Designation",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Designation of the requester"
            ),

            Map.of(
                    "name", "Phone Number",
                    "type", "decimal",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Phone number of the requester"
            ),

            Map.of(
                    "name", "New Radio",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Radio button field",
                    "canonicalValues", List.of("a")
            ),

            Map.of(
                    "name", "Text",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", true,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "Text input field"
            ),

            Map.of(
                    "name", "New Text Input 2",
                    "type", "string",
                    "mutability", "readWrite",
                    "returned", "default",
                    "required", false,
                    "multiValued", false,
                    "caseExact", false,
                    "description", "New text input field 2"
            )
    );

    public static String INCIDENT_FIELDS_SCHEMA = """
            {
                "type": "object",
                "properties": {
                  "Fill From Template": {
                    "type": "string",
                    "description": "Fill From Template"
                  },
                  "Requester": {
                    "type": "string",
                    "description": "Requester of the incident"
                  },
                  "Cc Emails": {
                    "type": "array",
                    "description": "CC email addresses",
                    "items": {
                      "type": "string"
                    }
                  },
                  "Subject": {
                    "type": "string",
                    "description": "Subject of the incident",
                    "placeholder": "Describe problem in short"
                  },
                  "Description": {
                    "type": "string",
                    "description": "Description of the incident"
                  },
                  "Status": {
                    "type": "string",
                    "description": "Status of the incident",
                    "enum": ["Open", "In Progress", "Resolved", "Closed"]
                  },
                  "Priority": {
                    "type": "string",
                    "description": "Priority level of the incident",
                    "enum": ["Low", "Medium", "High", "Critical"]
                  },
                  "Urgency": {
                    "type": "string",
                    "description": "Urgency level of the incident",
                    "enum": ["Low", "On User", "High", "Organisation"]
                  },
                  "Impact": {
                    "type": "string",
                    "description": "Impact level of the incident"
                  },
                  "Category": {
                    "type": "string",
                    "description": "Category of the incident"
                  },
                  "Technician Group": {
                    "type": "string",
                    "description": "Technician group assigned to the incident",
                    "readOnly": true
                  },
                  "Assignee": {
                    "type": "string",
                    "description": "Technician assigned to the incident"
                  },
                  "Department": {
                    "type": "string",
                    "description": "Department of the requester"
                  },
                  "Location": {
                    "type": "string",
                    "description": "Location related to the incident"
                  },
                  "Tags": {
                    "type": "array",
                    "description": "Tags associated with the incident",
                    "items": {
                      "type": "string"
                    }
                  },
                  "Attachment": {
                    "type": "array",
                    "description": "File attachments for the incident",
                    "items": {
                      "type": "string"
                    }
                  },
                  "Vendor": {
                    "type": "string",
                    "description": "Vendor associated with the incident"
                  },
                  "Transition Model": {
                    "type": "string",
                    "description": "Transition model for the incident",
                    "readOnly": true
                  },
                  "New Dropdown": {
                    "type": "string",
                    "description": "Custom dropdown field"
                  },
                  "New Text Area Test": {
                    "type": "string",
                    "description": "Custom text area field for testing"
                  },
                  "Categories": {
                    "type": "string",
                    "description": "Custom categories field",
                    "enum": ["Website", "HRMS", "DS"],
                    "readOnly": true
                  },
                  "Issue Repeated earlier": {
                    "type": "string",
                    "description": "Indicates if the issue was repeated earlier",
                    "enum": ["Yes", "NO", "Dont Konow"]
                  },
                  "test": {
                    "type": "string",
                    "description": "Test field",
                    "readOnly": true
                  },
                  "testfield": {
                    "type": "string",
                    "description": "Test field for validation"
                  },
                  "Tech GRP": {
                    "type": "string",
                    "description": "Technical group field"
                  },
                  "New Dependent": {
                    "type": "string",
                    "description": "Dependent field with hierarchical values",
                    "enum": ["ee", "a", "b"]
                  },
                  "My List": {
                    "type": "string",
                    "description": "Custom list field",
                    "enum": ["List1"]
                  },
                  "Testing 2": {
                    "type": "string",
                    "description": "Testing field 2"
                  },
                  "Designation": {
                    "type": "string",
                    "description": "Designation of the requester"
                  },
                  "Phone Number": {
                    "type": "number",
                    "description": "Phone number of the requester"
                  },
                  "New Radio": {
                    "type": "string",
                    "description": "Radio button field",
                    "enum": ["a"]
                  },
                  "Text": {
                    "type": "string",
                    "description": "Text input field"
                  },
                  "New Text Input 2": {
                    "type": "string",
                    "description": "New text input field 2"
                  }
                },
                "additionalProperties": false,
                "required": [
                  "Requester",
                  "Subject",
                  "Testing 2",
                  "Text"
                ]
              }""";
}
