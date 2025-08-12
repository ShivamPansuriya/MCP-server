#!/bin/bash

echo "=== Simple MCP Server Test ==="
echo

# Test 1: Health Check
echo "1. Health Check:"
curl -s http://localhost:8080/api/mcp/health
echo
echo

# Test 2: Get Session ID
echo "2. Getting Session ID from SSE:"
SESSION_RESPONSE=$(timeout 3 curl -N -H "Accept: text/event-stream" http://localhost:8080/sse 2>/dev/null | head -n 2)
SESSION_ID=$(echo "$SESSION_RESPONSE" | grep "data:" | sed 's/.*sessionId=\([^&]*\).*/\1/')
echo "Session ID: $SESSION_ID"
echo

if [ -n "$SESSION_ID" ]; then
    # Test 3: Get Current Time
    echo "3. Testing get_current_time tool:"
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d '{
            "jsonrpc": "2.0",
            "id": 1,
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

    # Test 4: Echo Tool
    echo "4. Testing echo tool:"
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d '{
            "jsonrpc": "2.0",
            "id": 2,
            "method": "tools/call",
            "params": {
                "name": "echo",
                "arguments": {
                    "text": "Hello MCP!"
                }
            }
        }' \
        "http://localhost:8080/mcp/message?sessionId=$SESSION_ID"
    echo
    echo

    # Test 5: List Tools
    echo "5. Listing available tools:"
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d '{
            "jsonrpc": "2.0",
            "id": 3,
            "method": "tools/list"
        }' \
        "http://localhost:8080/mcp/message?sessionId=$SESSION_ID"
    echo
    echo
else
    echo "Failed to get session ID"
fi

echo "=== Test Complete ==="
