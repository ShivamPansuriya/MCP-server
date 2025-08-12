# MCP Server with HttpServletSseServerTransportProvider

This project demonstrates how to implement a Model Context Protocol (MCP) server using the Java SDK's `HttpServletSseServerTransportProvider`. The server provides HTTP with Server-Sent Events (SSE) transport for MCP communication.

## Features

- **HttpServletSseServerTransportProvider**: Servlet-based MCP transport using SSE
- **Sample Tools**: Echo and calculator tools for demonstration
- **Sample Resources**: JSON data resources
- **REST API**: Management endpoints for monitoring and control
- **Spring Boot Integration**: Easy configuration and deployment

## Architecture

The MCP server consists of several key components:

1. **HttpServletSseServerTransportProvider**: Handles HTTP/SSE transport
2. **McpServerConfig**: Spring configuration for MCP components
3. **ExampleMcpHandler**: Implements MCP tools and resources
4. **McpServerService**: Manages server lifecycle and handler registration
5. **McpServerController**: REST API for server management

## Endpoints

### MCP Protocol Endpoints

- **SSE Endpoint**: `GET /sse` - Establishes Server-Sent Events connection
- **Message Endpoint**: `POST /mcp/message?sessionId={sessionId}` - Handles client messages

### Management REST API

- **Health Check**: `GET /api/mcp/health` - Server health status
- **Server Info**: `GET /api/mcp/info` - Server information and configuration
- **Server Status**: `GET /api/mcp/status` - Current server status
- **Send Notification**: `POST /api/mcp/notify` - Send notification to all clients

## Available Tools

1. **echo**: Echoes back the provided text
   - Parameters: `text` (string) - Text to echo back

2. **calculate**: Performs basic arithmetic calculations
   - Parameters: `expression` (string) - Mathematical expression (e.g., "2 + 3")

## Available Resources

1. **example://sample-data**: Sample JSON data with timestamp
2. **example://server-config**: Current server configuration

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

4. The server will start on port 8080

### Testing the Server

1. **Health Check**:
   ```bash
   curl http://localhost:8080/api/mcp/health
   ```

2. **Server Info**:
   ```bash
   curl http://localhost:8080/api/mcp/info
   ```

3. **Connect via SSE** (using curl):
   ```bash
   curl -N -H "Accept: text/event-stream" http://localhost:8080/sse
   ```

### MCP Client Connection

To connect an MCP client to this server:

1. **SSE Connection**: Connect to `http://localhost:8080/sse`
2. **Message Endpoint**: Use the endpoint URL provided in the SSE `endpoint` event
3. **Session Management**: Include the `sessionId` parameter in message requests

## Configuration

The server can be configured through `application.properties`:

```properties
# Server Configuration
server.port=8080

# Logging
logging.level.com.example.mcpserver=DEBUG
logging.level.io.modelcontextprotocol=DEBUG

# Application
spring.application.name=mcp-server
```

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
