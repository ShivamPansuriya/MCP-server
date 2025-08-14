# MCP Server with Streamable HTTP Transport

This project demonstrates how to implement a Model Context Protocol (MCP) server using the Java SDK's `HttpServletStreamableServerTransportProvider`. The server provides Streamable HTTP transport for MCP communication with a single endpoint.

## Features

- **HttpServletStreamableServerTransportProvider**: Servlet-based MCP transport using Streamable HTTP
- **Sample Tools**: Echo and calculator tools for demonstration
- **Sample Resources**: JSON data resources
- **REST API**: Management endpoints for monitoring and control
- **Spring Boot Integration**: Easy configuration and deployment

## Architecture

The MCP server consists of several key components:

1. **HttpServletStreamableServerTransportProvider**: Handles Streamable HTTP transport
2. **McpServerConfig**: Spring configuration for MCP components
3. **ExampleMcpHandler**: Implements MCP tools and resources
4. **McpServerService**: Manages server lifecycle and handler registration
5. **McpServerController**: REST API for server management

## Endpoints

### MCP Protocol Endpoints

- **Streamable HTTP Endpoint**: `GET|POST /mcp` - Single endpoint for all MCP communication
  - `GET /mcp` with `Accept: text/event-stream` - Establishes SSE connection
  - `POST /mcp` with JSON-RPC messages - Handles client requests

### Management REST API

- **Health Check**: `GET /health` - Server health status
- **Server Info**: `GET /info` - Server information and configuration
- **Server Status**: `GET /status` - Current server status

## Available Tools

1. **get_current_time**: Returns the current date and time
   - Parameters: `format` (string, optional) - Time format: 'iso' for ISO format, 'readable' for human-readable format

2. **echo**: Echoes back the provided text
   - Parameters: `text` (string, required) - Text to echo back

## Available Resources

Currently, this server focuses on tool implementation. Resources can be added by extending the server configuration.

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Running the Server

1. Clone the repository
2. Build the project:
   ```bash
   ./mvnw clean install
   ```

3. Run the server:
   ```bash
   ./mvnw spring-boot:run
   ```

4. The server will start on port 9092

### Testing the Server

1. **Health Check**:
   ```bash
   curl http://localhost:9092/health
   ```

2. **Server Info**:
   ```bash
   curl http://localhost:9092/info
   ```

3. **Connect via Streamable HTTP** (using curl):
   ```bash
   curl -N -H "Accept: text/event-stream" http://localhost:9092/mcp
   ```

### MCP Client Connection

To connect an MCP client to this server:

1. **Streamable HTTP**: Use transport type "Streamable HTTP"
2. **Endpoint URL**: `http://localhost:9092/mcp`
3. **Protocol**: The server supports both stateless POST requests and optional SSE streaming

## Configuration

The server can be configured through `application.properties`:

```properties
# Server Configuration
server.port=9092

# Logging
logging.level.com.example.mcpserver=DEBUG
logging.level.io.modelcontextprotocol=DEBUG

# Application
spring.application.name=mcp-server
```

## Streamable HTTP Implementation

This server implements the **Streamable HTTP** transport protocol as specified in the MCP specification. Key features:

### Transport Protocol Compliance
- ✅ **Single Endpoint**: Uses `/mcp` for all MCP communication
- ✅ **HTTP Methods**: Supports both GET and POST requests
- ✅ **Content Negotiation**: Handles `Accept` headers for JSON and SSE responses
- ✅ **Session Management**: Automatic session creation and management
- ✅ **Protocol Negotiation**: Supports MCP protocol versions 2025-03-26 and 2025-06-18

### MCP Inspector Compatibility
The server is fully compatible with MCP Inspector v0.15.0:
- **Transport Type**: Select "Streamable HTTP" in MCP Inspector
- **URL**: Use `http://localhost:9092/mcp`
- **Connection**: Automatic session establishment and tool discovery

### Protocol Features
- **JSON-RPC 2.0**: Full compliance with JSON-RPC message format
- **Tool Execution**: Supports `tools/list` and `tools/call` methods
- **Error Handling**: Proper error responses and validation
- **Streaming**: Optional SSE streaming for real-time communication

## Dependencies

The project uses the MCP Java SDK:

```xml
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp</artifactId>
    <version>0.11.0</version>
</dependency>
```

## Development

### Adding New Tools

1. Extend `ExampleMcpHandler.handleListTools()` to include your tool definition
2. Add a case in `ExampleMcpHandler.handleCallTool()` to handle your tool execution
3. Implement the tool logic

### Adding New Resources

1. Extend `ExampleMcpHandler.handleListResources()` to include your resource definition
2. Add a case in `ExampleMcpHandler.handleGetResource()` to provide your resource content

### Custom Transport Configuration

The `HttpServletSseServerTransportProvider` can be customized in `McpServerConfig`:

```java
@Bean
public HttpServletSseServerTransportProvider mcpTransportProvider(ObjectMapper objectMapper) {
    return HttpServletSseServerTransportProvider.builder()
            .objectMapper(objectMapper)
            .baseUrl("") 
            .messageEndpoint("/mcp/message")
            .sseEndpoint("/sse")
            .keepAliveInterval(Duration.ofSeconds(30))
            .build();
}
```

## Testing

Run the tests with:

```bash
./mvnw test
```

The tests verify:
- Spring context loading
- MCP server initialization
- REST API endpoints
- Server status and health

## Troubleshooting

1. **Port Already in Use**: Change the port in `application.properties`
2. **Connection Issues**: Check firewall settings and ensure the server is accessible
3. **Dependency Issues**: Verify Maven dependencies are correctly resolved

## License

This project is licensed under the MIT License.
# MCP-server
