# MCP Server Two-Tool Workflow Example

This document demonstrates the restructured workflow for updating incidents using the MCP server.

## Step 1: Get Field Structure

First, call `get_updatable_fields` to understand the available fields and their constraints:

**Request:**
```json
{
  "method": "tools/call",
  "params": {
    "name": "get_updatable_fields",
    "arguments": {}
  }
}
```

**Response:**
```json
{
  "attributes": [
    {
      "name": "title",
      "type": "string",
      "mutability": "readWrite",
      "returned": "default",
      "required": false,
      "multiValued": false,
      "caseExact": false,
      "description": "Short title of the incident",
      "maxLength": 200
    },
    {
      "name": "description",
      "type": "string",
      "mutability": "readWrite",
      "returned": "default",
      "required": false,
      "multiValued": false,
      "caseExact": false,
      "description": "Detailed description of the incident",
      "maxLength": 2000
    },
    {
      "name": "priority",
      "type": "string",
      "mutability": "readWrite",
      "returned": "default",
      "required": false,
      "multiValued": false,
      "caseExact": false,
      "description": "Priority level of the incident",
      "canonicalValues": ["Low", "Medium", "High", "Critical"]
    },
    {
      "name": "status",
      "type": "string",
      "mutability": "readWrite",
      "returned": "default",
      "required": false,
      "multiValued": false,
      "caseExact": false,
      "description": "Status of the incident",
      "canonicalValues": ["Open", "In Progress", "Resolved", "Closed"]
    }
  ]
}
```

## Step 2: Update Incident

Now use the simplified `update_incident` tool with just the incident name and update data:

**Request:**
```json
{
  "method": "tools/call",
  "params": {
    "name": "update_incident",
    "arguments": {
      "name": "INC-12345678",
      "updates": {
        "title": "Updated incident title",
        "priority": "High",
        "status": "In Progress"
      }
    }
  }
}
```

**Response:**
```json
{
  "message": "Incident updated successfully",
  "updated_incident": {
    "incident_id": "INC-12345678",
    "title": "Updated incident title",
    "description": "Original description",
    "priority": "High",
    "status": "In Progress",
    "requester": "user@example.com",
    "created_at": "2025-08-18T11:30:00Z"
  }
}
```

## Key Changes

1. **GetUpdatableFieldsTool**: Now returns SCIM-like structured metadata with detailed field information
2. **UpdateIncidentTool**: Simplified to accept only `name` and `updates` parameters
3. **Workflow**: Client agents should first call `get_updatable_fields` to understand the schema, then use `update_incident` with the appropriate data structure

## Benefits

- **Separation of Concerns**: Schema discovery is separate from update operations
- **Rich Metadata**: Detailed field information including constraints and validation rules
- **Simplified Updates**: Update tool has minimal schema complexity
- **Standardized Format**: Uses SCIM-like attribute structure for consistency
