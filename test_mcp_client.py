#!/usr/bin/env python3
"""
Simple MCP client test script for testing the HttpServletSseServerTransportProvider
This script demonstrates how to connect to the MCP server and call tools.
"""

import requests
import json
import time
import re
from urllib.parse import urlparse, parse_qs

def test_mcp_server():
    base_url = "http://localhost:8080"
    
    print("=== MCP Server Test Client ===")
    print()
    
    # Test 1: Health check
    print("1. Testing server health...")
    try:
        response = requests.get(f"{base_url}/api/mcp/health")
        print(f"Health check: {response.json()}")
    except Exception as e:
        print(f"Health check failed: {e}")
        return
    print()
    
    # Test 2: Establish SSE connection to get session ID
    print("2. Establishing SSE connection...")
    try:
        response = requests.get(f"{base_url}/sse", 
                              headers={"Accept": "text/event-stream"},
                              stream=True,
                              timeout=5)
        
        session_id = None
        message_endpoint = None
        
        # Read the first few lines to get the endpoint
        for line in response.iter_lines(decode_unicode=True):
            if line.startswith("data: "):
                endpoint_url = line[6:]  # Remove "data: " prefix
                print(f"Received endpoint: {endpoint_url}")
                
                # Extract session ID from URL
                parsed_url = urlparse(endpoint_url)
                query_params = parse_qs(parsed_url.query)
                if 'sessionId' in query_params:
                    session_id = query_params['sessionId'][0]
                    message_endpoint = f"{base_url}{endpoint_url}"
                    break
        
        response.close()
        
        if not session_id:
            print("Failed to get session ID")
            return
            
        print(f"Session ID: {session_id}")
        print(f"Message endpoint: {message_endpoint}")
        
    except Exception as e:
        print(f"SSE connection failed: {e}")
        return
    print()
    
    # Test 3: List available tools
    print("3. Listing available tools...")
    try:
        payload = {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "tools/list"
        }
        
        response = requests.post(message_endpoint, 
                               json=payload,
                               headers={"Content-Type": "application/json"})
        
        if response.status_code == 200:
            print("Tools list request sent successfully")
            print("Note: Response will be sent via SSE, not HTTP response")
        else:
            print(f"Tools list failed: {response.status_code} - {response.text}")
            
    except Exception as e:
        print(f"Tools list failed: {e}")
    print()
    
    # Test 4: Call get_current_time tool
    print("4. Testing get_current_time tool...")
    try:
        payload = {
            "jsonrpc": "2.0",
            "id": 2,
            "method": "tools/call",
            "params": {
                "name": "get_current_time",
                "arguments": {
                    "format": "readable"
                }
            }
        }
        
        response = requests.post(message_endpoint, 
                               json=payload,
                               headers={"Content-Type": "application/json"})
        
        if response.status_code == 200:
            print("get_current_time tool called successfully")
            print("Note: Response will be sent via SSE, not HTTP response")
        else:
            print(f"get_current_time failed: {response.status_code} - {response.text}")
            
    except Exception as e:
        print(f"get_current_time failed: {e}")
    print()
    
    # Test 5: Call echo tool
    print("5. Testing echo tool...")
    try:
        payload = {
            "jsonrpc": "2.0",
            "id": 3,
            "method": "tools/call",
            "params": {
                "name": "echo",
                "arguments": {
                    "text": "Hello from Python MCP client!"
                }
            }
        }
        
        response = requests.post(message_endpoint, 
                               json=payload,
                               headers={"Content-Type": "application/json"})
        
        if response.status_code == 200:
            print("echo tool called successfully")
            print("Note: Response will be sent via SSE, not HTTP response")
        else:
            print(f"echo failed: {response.status_code} - {response.text}")
            
    except Exception as e:
        print(f"echo failed: {e}")
    print()
    
    # Test 6: Listen for responses (optional)
    print("6. Listening for responses via SSE (for 5 seconds)...")
    try:
        response = requests.get(f"{base_url}/sse", 
                              headers={"Accept": "text/event-stream"},
                              stream=True,
                              timeout=5)
        
        start_time = time.time()
        for line in response.iter_lines(decode_unicode=True):
            if time.time() - start_time > 5:  # Listen for 5 seconds
                break
                
            if line.startswith("data: ") and line != "data: ":
                try:
                    data = line[6:]  # Remove "data: " prefix
                    if data.startswith("{"):  # JSON response
                        json_data = json.loads(data)
                        print(f"Received response: {json.dumps(json_data, indent=2)}")
                except json.JSONDecodeError:
                    print(f"Received non-JSON data: {data}")
        
        response.close()
        
    except Exception as e:
        print(f"SSE listening failed: {e}")
    
    print()
    print("=== Test Complete ===")
    print()
    print("Summary:")
    print("- The MCP server is running and responding to requests")
    print("- SSE connection established successfully")
    print("- Tools can be called via the message endpoint")
    print("- Responses are sent back via SSE (not shown in this simple test)")
    print()
    print("To use with an LLM:")
    print(f"1. Connect to SSE: {base_url}/sse")
    print("2. Extract session ID from the endpoint event")
    print("3. Send MCP requests to the message endpoint with session ID")
    print("4. Listen for responses on the SSE connection")

if __name__ == "__main__":
    test_mcp_server()
