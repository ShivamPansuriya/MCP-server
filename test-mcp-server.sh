#!/bin/bash

# Test script for MCP Server with HttpServletSseServerTransportProvider
# This script demonstrates how to interact with the MCP server

echo "=== MCP Server Test Script ==="
echo

# Test 1: Health Check
echo "1. Testing Health Check:"
curl -s http://localhost:8080/api/mcp/health | jq '.'
echo

# Test 2: Server Info
echo "2. Testing Server Info:"
curl -s http://localhost:8080/api/mcp/info | jq '.'
echo

# Test 3: Server Status
echo "3. Testing Server Status:"
curl -s http://localhost:8080/api/mcp/status | jq '.'
echo

# Test 4: SSE Connection (get session ID)
echo "4. Testing SSE Connection (getting session ID):"
echo "Connecting to SSE endpoint..."
SESSION_RESPONSE=$(timeout 3 curl -N -H "Accept: text/event-stream" http://localhost:8080/sse 2>/dev/null | head -n 2)
echo "SSE Response:"
echo "$SESSION_RESPONSE"

# Extract session ID from the endpoint URL
SESSION_ID=$(echo "$SESSION_RESPONSE" | grep "data:" | sed 's/.*sessionId=\([^&]*\).*/\1/')
echo "Extracted Session ID: $SESSION_ID"
echo

if [ -n "$SESSION_ID" ]; then
    # Test 5: List Tools
    echo "5. Testing List Tools:"
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d '{
            "jsonrpc": "2.0",
            "id": 1,
            "method": "tools/list"
        }' \
        "http://localhost:8080/mcp/message?sessionId=$SESSION_ID"
    echo
    echo

    # Test 6: Call get_current_time tool
    echo "6. Testing get_current_time tool (readable format):"
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d '{
            "jsonrpc": "2.0",
            "id": 2,
            "method": "tools/call",
            "params": {
                "name": "get_current_time",
                "arguments": {
                    "format": "readable"
                }
            }
        }' \
        "http://localhost:8080/mcp/message?sessionId=$SESSION_ID"
    echo
    echo

    # Test 7: Call get_current_time tool with ISO format
    echo "7. Testing get_current_time tool (ISO format):"
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d '{
            "jsonrpc": "2.0",
            "id": 3,
            "method": "tools/call",
            "params": {
                "name": "get_current_time",
                "arguments": {
                    "format": "iso"
                }
            }
        }' \
        "http://localhost:8080/mcp/message?sessionId=$SESSION_ID"
    echo
    echo

    # Test 8: Call echo tool
    echo "8. Testing echo tool:"
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d '{
            "jsonrpc": "2.0",
            "id": 4,
            "method": "tools/call",
            "params": {
                "name": "echo",
                "arguments": {
                    "text": "Hello from MCP Server!"
                }
            }
        }' \
        "http://localhost:8080/mcp/message?sessionId=$SESSION_ID"
    echo
    echo

    # Test 9: Test error handling (missing required parameter)
    echo "9. Testing error handling (echo without text parameter):"
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d '{
            "jsonrpc": "2.0",
            "id": 5,
            "method": "tools/call",
            "params": {
                "name": "echo",
                "arguments": {}
            }
        }' \
        "http://localhost:8080/mcp/message?sessionId=$SESSION_ID"
    echo
    echo
else
    echo "Failed to get session ID. Make sure the server is running."
fi

echo "=== Test Complete ==="
echo
echo "To test with an LLM client:"
echo "1. Connect to SSE endpoint: http://localhost:8080/sse"
echo "2. Use the provided message endpoint with session ID"
echo "3. Available tools:"
echo "   - get_current_time: Returns current date/time"
echo "   - echo: Echoes back provided text"
echo
echo "Example MCP client connection:"
echo "  SSE URL: http://localhost:8080/sse"
echo "  Message URL: http://localhost:8080/mcp/message?sessionId=<session_id>"
