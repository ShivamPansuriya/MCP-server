# MCP Server Architecture Implementation Guide

This comprehensive guide provides detailed instructions for migrating your current MCP server from a monolithic configuration approach to a modular, extensible architecture.

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Phase-by-Phase Implementation](#phase-by-phase-implementation)
4. [Package Structure](#package-structure)
5. [Core Interface Definitions](#core-interface-definitions)
6. [Implementation Examples](#implementation-examples)
7. [Testing Strategy](#testing-strategy)
8. [Migration Checklists](#migration-checklists)

## Overview

The migration transforms your current `McpServerConfig.java` monolithic approach into a layered architecture with:

- **Tool Framework**: Pluggable tool system with registration and discovery
- **Execution Engine**: Centralized tool execution with validation and interceptors
- **Configuration Management**: Multi-source configuration with validation
- **Extension System**: Plugin architecture for custom tools
- **Infrastructure**: Error handling, logging, metrics, and security

## Prerequisites

Before starting the migration, ensure you have:

- [ ] Java 17+ installed
- [ ] Spring Boot 3.x knowledge
- [ ] Understanding of dependency injection patterns
- [ ] Familiarity with the current MCP server codebase
- [ ] Git repository for version control

## Phase-by-Phase Implementation

### Phase 1: Extract Tool Definitions (Week 1)

**Goal**: Extract hardcoded tools from `McpServerConfig.java` into separate classes.

#### 1.1 Create Base Tool Interface

Create the core tool interface that all tools will implement:

```java
// src/main/java/com/example/mcpserver/tool/api/McpTool.java
package com.example.mcpserver.tool.api;

import java.util.Map;

public interface McpTool {
    String getName();
    String getDescription();
    String getSchema();
    ToolMetadata getMetadata();
    ValidationResult validate(Map<String, Object> arguments);
    ToolResult execute(ToolContext context, Map<String, Object> arguments);
}
```

#### 1.2 Create Supporting Classes

```java
// src/main/java/com/example/mcpserver/tool/api/ToolMetadata.java
package com.example.mcpserver.tool.api;

import java.util.Set;
import java.util.Map;

public class ToolMetadata {
    private final String name;
    private final String description;
    private final String version;
    private final String category;
    private final Set<String> tags;
    private final String schema;
    private final Map<String, Object> configuration;
    
    // Constructor, getters, and builder pattern
    public static ToolMetadataBuilder builder() {
        return new ToolMetadataBuilder();
    }
}
```

#### 1.3 Extract Current Tools

Transform your existing tools from `McpServerConfig.java`:

**Before (in McpServerConfig.java):**
```java
.toolCall(getCurrentTimeTool, (exchange, request) -> {
    Map<String, Object> arguments = request.arguments();
    String format = (String) arguments.getOrDefault("format", "readable");
    // ... implementation
})
```

**After (new GetCurrentTimeTool.java):**
```java
// src/main/java/com/example/mcpserver/tools/builtin/GetCurrentTimeTool.java
package com.example.mcpserver.tools.builtin;

import com.example.mcpserver.tool.api.*;
import org.springframework.stereotype.Component;

@Component
public class GetCurrentTimeTool implements McpTool {
    
    private static final String SCHEMA = """
        {
            "$schema": "http://json-schema.org/draft-07/schema#",
            "type": "object",
            "properties": {
                "format": {
                    "type": "string",
                    "description": "Optional time format: 'iso' or 'readable'",
                    "enum": ["iso", "readable"]
                }
            },
            "required": []
        }
        """;
    
    @Override
    public String getName() {
        return "get_current_time";
    }
    
    @Override
    public String getDescription() {
        return "Returns the current date and time";
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
            .category("utility")
            .schema(getSchema())
            .build();
    }
    
    @Override
    public ValidationResult validate(Map<String, Object> arguments) {
        // Validation logic
        return ValidationResult.success();
    }
    
    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> arguments) {
        String format = (String) arguments.getOrDefault("format", "readable");
        LocalDateTime now = LocalDateTime.now();
        String timeString;
        
        if ("iso".equals(format)) {
            timeString = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else {
            timeString = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        return ToolResult.success(
            List.of(new McpSchema.TextContent("Current time: " + timeString))
        );
    }
}
```

#### 1.4 Phase 1 Checklist

- [ ] Create `tool/api` package structure
- [ ] Implement `McpTool` interface
- [ ] Create `ToolMetadata`, `ToolResult`, `ValidationResult` classes
- [ ] Extract `GetCurrentTimeTool` from config
- [ ] Extract `EchoTool` from config
- [ ] Update existing tests to work with new structure
- [ ] Verify tools still function correctly

### Phase 2: Implement Tool Registry and Factory (Week 2)

**Goal**: Create a centralized registry for tool management and factory for tool creation.

#### 2.1 Create Tool Registry

```java
// src/main/java/com/example/mcpserver/tool/registry/ToolRegistry.java
package com.example.mcpserver.tool.registry;

import com.example.mcpserver.tool.api.*;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ToolRegistry {
    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> categories = new ConcurrentHashMap<>();
    private final List<ToolRegistryListener> listeners = new ArrayList<>();
    
    public void register(ToolDefinition definition) {
        String name = definition.getMetadata().getName();
        tools.put(name, definition);
        
        String category = definition.getMetadata().getCategory();
        categories.computeIfAbsent(category, k -> new HashSet<>()).add(name);
        
        notifyListeners(listener -> listener.onToolRegistered(definition));
    }
    
    public Optional<ToolDefinition> findByName(String name) {
        return Optional.ofNullable(tools.get(name));
    }
    
    public List<ToolDefinition> getAllTools() {
        return new ArrayList<>(tools.values());
    }
    
    public List<ToolDefinition> findByCategory(String category) {
        return categories.getOrDefault(category, Collections.emptySet())
            .stream()
            .map(tools::get)
            .filter(Objects::nonNull)
            .toList();
    }
    
    public boolean isRegistered(String name) {
        return tools.containsKey(name);
    }
    
    public void addListener(ToolRegistryListener listener) {
        listeners.add(listener);
    }
    
    private void notifyListeners(Consumer<ToolRegistryListener> action) {
        listeners.forEach(action);
    }
}
```

#### 2.2 Create Tool Factory

```java
// src/main/java/com/example/mcpserver/tool/factory/DefaultToolFactory.java
package com.example.mcpserver.tool.factory;

import com.example.mcpserver.tool.api.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class DefaultToolFactory implements ToolFactory {
   
    
    @Override
    public McpTool getTool(ToolDefinition definition) {
        // use factory pattern to create/get tool *[do not use reflection]
    }
    
    @Override
    public boolean supports(Class<? extends McpTool> toolClass) {
        // check if factory can create tool
    }
}
```

#### 2.3 Update McpServerConfig

Modify your existing `McpServerConfig.java` to use the registry:

```java
// Updated McpServerConfig.java
@Configuration
public class McpServerConfig {
    
    private final ToolRegistry toolRegistry;
    
    public McpServerConfig(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }
    
    @Bean
    public McpSyncServer mcpServer(HttpServletSseServerTransportProvider transportProvider) {
        McpServer.Builder builder = McpServer.sync(transportProvider)
            .serverInfo("Example MCP Server", "1.0.0")
            .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build());
        
        // Register tools from registry
        for (ToolDefinition toolDef : toolRegistry.getAllTools()) {
            McpSchema.Tool mcpTool = new McpSchema.Tool(
                toolDef.getMetadata().getName(),
                toolDef.getMetadata().getDescription(),
                toolDef.getMetadata().getSchema()
            );
            
            builder.toolCall(mcpTool, (exchange, request) -> {
                // Delegate to tool execution engine (Phase 3)
                return executeToolViaRegistry(toolDef.getMetadata().getName(), request);
            });
        }
        
        return builder.build();
    }
    
    private McpSchema.CallToolResult executeToolViaRegistry(String toolName, 
            McpSchema.CallToolRequest request) {
        // Temporary implementation - will be replaced in Phase 3
        Optional<ToolDefinition> toolDef = toolRegistry.findByName(toolName);
        if (toolDef.isEmpty()) {
            return new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent("Tool not found: " + toolName)),
                true
            );
        }
        
        // Create and execute tool
        // This will be moved to ToolExecutionEngine in Phase 3
        return null; // Placeholder
    }
}
```

#### 2.4 Phase 2 Checklist

- [ ] Create `ToolRegistry` class
- [ ] Create `ToolFactory` interface and `DefaultToolFactory`
- [ ] Create `ToolDefinition` class
- [ ] Create `ToolRegistryListener` interface
- [ ] Update `McpServerConfig` to use registry
- [ ] Create tool discovery service
- [ ] Register existing tools in registry
- [ ] Test tool registration and lookup
- [ ] Verify MCP server still starts correctly

### Phase 3: Add Tool Discovery and Lifecycle Management (Week 3)

**Goal**: Implement automatic tool discovery and lifecycle management.

#### 3.1 Create Tool Discovery Service

```java
// src/main/java/com/example/mcpserver/tool/discovery/ToolDiscoveryService.java
package com.example.mcpserver.tool.discovery;

import com.example.mcpserver.tool.api.*;
import com.example.mcpserver.tool.registry.ToolRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;

@Service
public class ToolDiscoveryService {
    private final ToolRegistry toolRegistry;
    private final ApplicationContext applicationContext;
    private final List<ToolScanner> scanners;
    
    @Autowired
    public ToolDiscoveryService(ToolRegistry toolRegistry, 
                               ApplicationContext applicationContext,
                               List<ToolScanner> scanners) {
        this.toolRegistry = toolRegistry;
        this.applicationContext = applicationContext;
        this.scanners = scanners;
    }
    
    @PostConstruct
    public void discoverTools() {
        // Discover Spring-managed tools
        Map<String, McpTool> toolBeans = applicationContext.getBeansOfType(McpTool.class);
        
        for (McpTool tool : toolBeans.values()) {
            ToolDefinition definition = createDefinitionFromTool(tool);
            toolRegistry.register(definition);
        }
        
        // Use scanners for additional discovery
        for (ToolScanner scanner : scanners) {
            List<ToolDefinition> discovered = scanner.scan("com.example.mcpserver.tools");
            discovered.forEach(toolRegistry::register);
        }
    }
    
    private ToolDefinition createDefinitionFromTool(McpTool tool) {
        return ToolDefinition.builder()
            .metadata(tool.getMetadata())
            .toolClass(tool.getClass())
            .status(ToolStatus.ENABLED)
            .build();
    }
}
```

#### 3.2 Create Lifecycle Manager

```java
// src/main/java/com/example/mcpserver/tool/lifecycle/ToolLifecycleManager.java
package com.example.mcpserver.tool.lifecycle;

import com.example.mcpserver.tool.api.*;
import org.springframework.stereotype.Component;

@Component
public class ToolLifecycleManager {
    private final ToolRegistry toolRegistry;
    private final ToolHealthChecker healthChecker;
    
    public ToolLifecycleManager(ToolRegistry toolRegistry, 
                               ToolHealthChecker healthChecker) {
        this.toolRegistry = toolRegistry;
        this.healthChecker = healthChecker;
    }
    
    public void initialize(McpTool tool) {
        if (tool instanceof LifecycleAware) {
            ((LifecycleAware) tool).initialize();
        }
    }
    
    public void destroy(McpTool tool) {
        if (tool instanceof LifecycleAware) {
            ((LifecycleAware) tool).destroy();
        }
    }
    
    public HealthStatus checkHealth(String toolName) {
        return toolRegistry.findByName(toolName)
            .map(def -> healthChecker.checkHealth(def))
            .orElse(HealthStatus.UNKNOWN);
    }
    
    public void refreshTool(String toolName) {
        toolRegistry.findByName(toolName).ifPresent(definition -> {
            // Recreate tool instance
            definition.setStatus(ToolStatus.REFRESHING);
            // Implementation details...
            definition.setStatus(ToolStatus.ENABLED);
        });
    }
}
```

#### 3.3 Phase 3 Checklist

- [ ] Create `ToolDiscoveryService`
- [ ] Create `ToolLifecycleManager`
- [ ] Create `ToolScanner` interface and implementations
- [ ] Create `LifecycleAware` interface for tools
- [ ] Create `ToolHealthChecker`
- [ ] Implement automatic tool discovery on startup
- [ ] Add lifecycle callbacks to existing tools
- [ ] Test tool discovery and lifecycle management
- [ ] Verify health checking works

### Phase 4: Implement Execution Engine with Interceptors (Week 4)

**Goal**: Create centralized tool execution with validation and interceptor support.

#### 4.1 Create Tool Execution Engine

```java
// src/main/java/com/example/mcpserver/tool/execution/ToolExecutionEngine.java
package com.example.mcpserver.tool.execution;

import com.example.mcpserver.tool.api.*;
import com.example.mcpserver.tool.registry.ToolRegistry;
import org.springframework.stereotype.Component;

@Component
public class ToolExecutionEngine {
    private final ToolRegistry toolRegistry;
    private final ToolValidator validator;
    private final List<ToolInterceptor> interceptors;
    private final ToolFactory toolFactory;
    
    public ToolExecutionEngine(ToolRegistry toolRegistry,
                              ToolValidator validator,
                              List<ToolInterceptor> interceptors,
                              ToolFactory toolFactory) {
        this.toolRegistry = toolRegistry;
        this.validator = validator;
        this.interceptors = interceptors.stream()
            .sorted(Comparator.comparing(ToolInterceptor::getOrder))
            .toList();
        this.toolFactory = toolFactory;
    }
    
    public ToolResult execute(ToolExecutionRequest request) {
        ToolContext context = createContext(request);
        
        try {
            // Find tool definition
            Optional<ToolDefinition> toolDef = toolRegistry.findByName(request.getToolName());
            if (toolDef.isEmpty()) {
                return ToolResult.error("Tool not found: " + request.getToolName());
            }
            
            // Create tool instance
            McpTool tool = toolFactory.createTool(toolDef.get());
            
            // Validate arguments
            ValidationResult validation = validator.validate(tool, request.getArguments());
            if (!validation.isValid()) {
                return ToolResult.error("Validation failed: " + validation.getErrors());
            }
            
            // Apply pre-execution interceptors
            applyPreInterceptors(context, tool, request.getArguments());
            
            // Execute tool
            ToolResult result = tool.execute(context, request.getArguments());
            
            // Apply post-execution interceptors
            result = applyPostInterceptors(context, tool, result);
            
            return result;
            
        } catch (Exception e) {
            // Apply error interceptors
            applyErrorInterceptors(context, null, e);
            return ToolResult.error("Execution failed: " + e.getMessage());
        }
    }
    
    private ToolContext createContext(ToolExecutionRequest request) {
        return ToolContext.builder()
            .sessionId(request.getSessionId())
            .requestId(request.getRequestId())
            .clientInfo(request.getClientInfo())
            .build();
    }
    
    private void applyPreInterceptors(ToolContext context, McpTool tool, 
                                     Map<String, Object> arguments) {
        for (ToolInterceptor interceptor : interceptors) {
            interceptor.preExecute(context, tool, arguments);
        }
    }
    
    private ToolResult applyPostInterceptors(ToolContext context, McpTool tool, 
                                           ToolResult result) {
        ToolResult currentResult = result;
        for (ToolInterceptor interceptor : interceptors) {
            currentResult = interceptor.postExecute(context, tool, currentResult);
        }
        return currentResult;
    }
    
    private void applyErrorInterceptors(ToolContext context, McpTool tool, Exception e) {
        for (ToolInterceptor interceptor : interceptors) {
            interceptor.onError(context, tool, e);
        }
    }
}
```

#### 4.2 Create Interceptors

```java
// src/main/java/com/example/mcpserver/tool/execution/interceptor/LoggingInterceptor.java
package com.example.mcpserver.tool.execution.interceptor;

import com.example.mcpserver.tool.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingInterceptor implements ToolInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);
    
    @Override
    public void preExecute(ToolContext context, McpTool tool, Map<String, Object> arguments) {
        logger.info("Executing tool: {} with session: {}", 
            tool.getName(), context.getSessionId());
    }
    
    @Override
    public ToolResult postExecute(ToolContext context, McpTool tool, ToolResult result) {
        logger.info("Tool {} execution completed. Success: {}", 
            tool.getName(), !result.isError());
        return result;
    }
    
    @Override
    public void onError(ToolContext context, McpTool tool, Exception exception) {
        logger.error("Tool {} execution failed in session {}: {}", 
            tool != null ? tool.getName() : "unknown", 
            context.getSessionId(), 
            exception.getMessage(), exception);
    }
    
    @Override
    public int getOrder() {
        return 100; // Execute after security but before metrics
    }
}
```

#### 4.3 Update McpServerConfig to Use Execution Engine

```java
// Updated portion of McpServerConfig.java
@Bean
public McpSyncServer mcpServer(HttpServletSseServerTransportProvider transportProvider,
                              ToolExecutionEngine executionEngine) {
    McpServer.Builder builder = McpServer.sync(transportProvider)
        .serverInfo("Example MCP Server", "1.0.0")
        .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build());
    
    // Register tools from registry
    for (ToolDefinition toolDef : toolRegistry.getAllTools()) {
        McpSchema.Tool mcpTool = new McpSchema.Tool(
            toolDef.getMetadata().getName(),
            toolDef.getMetadata().getDescription(),
            toolDef.getMetadata().getSchema()
        );
        
        builder.toolCall(mcpTool, (exchange, request) -> {
            ToolExecutionRequest execRequest = ToolExecutionRequest.builder()
                .toolName(toolDef.getMetadata().getName())
                .arguments(request.arguments())
                .sessionId(extractSessionId(exchange))
                .requestId(generateRequestId())
                .build();
            
            ToolResult result = executionEngine.execute(execRequest);
            
            return new McpSchema.CallToolResult(
                result.getContent(),
                result.isError()
            );
        });
    }
    
    return builder.build();
}
```

#### 4.4 Phase 4 Checklist

- [ ] Create `ToolExecutionEngine`
- [ ] Create `ToolExecutionRequest` and builder
- [ ] Create `ToolInterceptor` interface
- [ ] Implement `LoggingInterceptor`
- [ ] Implement `MetricsInterceptor`
- [ ] Implement `SecurityInterceptor`
- [ ] Create `ToolValidator`
- [ ] Update `McpServerConfig` to use execution engine
- [ ] Test tool execution with interceptors
- [ ] Verify logging and metrics collection

### Phase 5: Configuration Management System (Week 5)

**Goal**: Implement multi-source configuration management with validation and caching.

#### 5.1 Create Configuration Manager

```java
// src/main/java/com/example/mcpserver/config/ConfigurationManager.java
package com.example.mcpserver.config;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConfigurationManager {
    private final List<ConfigurationSource> sources;
    private final ConfigurationCache cache;
    private final ConfigurationValidator validator;
    private final List<ConfigurationListener> listeners = new ArrayList<>();

    public ConfigurationManager(List<ConfigurationSource> sources,
                               ConfigurationCache cache,
                               ConfigurationValidator validator) {
        this.sources = sources.stream()
            .sorted(Comparator.comparing(ConfigurationSource::getPriority))
            .toList();
        this.cache = cache;
        this.validator = validator;
    }

    public <T> Optional<T> getConfiguration(String key, Class<T> type) {
        // Check cache first
        Optional<T> cached = cache.get(key, type);
        if (cached.isPresent()) {
            return cached;
        }

        // Load from sources
        for (ConfigurationSource source : sources) {
            Map<String, Object> config = source.load();
            if (config.containsKey(key)) {
                T value = convertValue(config.get(key), type);
                cache.put(key, value);
                return Optional.of(value);
            }
        }

        return Optional.empty();
    }

    public ToolConfiguration getToolConfiguration(String toolName) {
        Map<String, Object> toolConfig = new HashMap<>();

        // Merge configuration from all sources
        for (ConfigurationSource source : sources) {
            Map<String, Object> sourceConfig = source.load();
            String toolKey = "tools." + toolName;
            if (sourceConfig.containsKey(toolKey)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> toolSpecific = (Map<String, Object>) sourceConfig.get(toolKey);
                toolConfig.putAll(toolSpecific);
            }
        }

        return ToolConfiguration.fromMap(toolName, toolConfig);
    }

    public void refresh() {
        cache.clear();
        sources.stream()
            .filter(ConfigurationSource::isReloadable)
            .forEach(source -> {
                try {
                    source.load();
                } catch (Exception e) {
                    // Log error but continue
                }
            });

        notifyListeners(ConfigurationChangeEvent.refresh());
    }

    private void notifyListeners(ConfigurationChangeEvent event) {
        listeners.forEach(listener -> listener.onConfigurationChanged(event));
    }
}
```

#### 5.2 Create Configuration Sources

```java
// src/main/java/com/example/mcpserver/config/source/YamlConfigurationSource.java
package com.example.mcpserver.config.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.Map;

@Component
public class YamlConfigurationSource implements ConfigurationSource {
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final String filePath;

    public YamlConfigurationSource(@Value("${mcp.config.yaml.path:config/tools.yml}") String filePath) {
        this.filePath = filePath;
    }

    @Override
    public Map<String, Object> load() {
        try {
            File configFile = new File(filePath);
            if (configFile.exists()) {
                return yamlMapper.readValue(configFile, Map.class);
            }
        } catch (Exception e) {
            throw new ConfigurationException("Failed to load YAML configuration from: " + filePath, e);
        }
        return Collections.emptyMap();
    }

    @Override
    public boolean supports(String source) {
        return source.endsWith(".yml") || source.endsWith(".yaml");
    }

    @Override
    public int getPriority() {
        return 200; // Higher priority than properties
    }

    @Override
    public boolean isReloadable() {
        return true;
    }
}
```

#### 5.3 Create Tool Configuration

```java
// src/main/java/com/example/mcpserver/config/ToolConfiguration.java
package com.example.mcpserver.config;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class ToolConfiguration {
    private final String name;
    private final boolean enabled;
    private final Map<String, Object> properties;
    private final Duration timeout;
    private final RetryPolicy retryPolicy;
    private final SecurityConfiguration security;

    private ToolConfiguration(Builder builder) {
        this.name = builder.name;
        this.enabled = builder.enabled;
        this.properties = Map.copyOf(builder.properties);
        this.timeout = builder.timeout;
        this.retryPolicy = builder.retryPolicy;
        this.security = builder.security;
    }

    public static ToolConfiguration fromMap(String name, Map<String, Object> config) {
        Builder builder = builder(name);

        builder.enabled((Boolean) config.getOrDefault("enabled", true));

        if (config.containsKey("timeout")) {
            builder.timeout(Duration.parse((String) config.get("timeout")));
        }

        if (config.containsKey("properties")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> props = (Map<String, Object>) config.get("properties");
            builder.properties(props);
        }

        if (config.containsKey("security")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> secConfig = (Map<String, Object>) config.get("security");
            builder.security(SecurityConfiguration.fromMap(secConfig));
        }

        return builder.build();
    }

    public <T> Optional<T> getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    // Getters and builder
    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private boolean enabled = true;
        private Map<String, Object> properties = Map.of();
        private Duration timeout = Duration.ofSeconds(30);
        private RetryPolicy retryPolicy = RetryPolicy.none();
        private SecurityConfiguration security = SecurityConfiguration.none();

        private Builder(String name) {
            this.name = name;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder security(SecurityConfiguration security) {
            this.security = security;
            return this;
        }

        public ToolConfiguration build() {
            return new ToolConfiguration(this);
        }
    }
}
```

#### 5.4 Phase 5 Checklist

- [ ] Create `ConfigurationManager`
- [ ] Create `ConfigurationSource` interface
- [ ] Implement `YamlConfigurationSource`
- [ ] Implement `PropertiesConfigurationSource`
- [ ] Implement `EnvironmentConfigurationSource`
- [ ] Create `ToolConfiguration` class
- [ ] Create `SecurityConfiguration` class
- [ ] Create `ConfigurationCache` implementation
- [ ] Create `ConfigurationValidator`
- [ ] Create sample configuration files
- [ ] Test configuration loading and merging
- [ ] Test configuration refresh functionality

### Phase 6: Extension Points and Plugin System (Week 6)

**Goal**: Implement plugin architecture for extensibility.

#### 6.1 Create Plugin Manager

```java
// src/main/java/com/example/mcpserver/extension/plugin/PluginManager.java
package com.example.mcpserver.extension.plugin;

import com.example.mcpserver.extension.ExtensionPoint;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PluginManager {
    private final Map<String, ExtensionPoint<?>> extensionPoints = new ConcurrentHashMap<>();
    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();
    private final PluginLoader pluginLoader;
    private final ExtensionRegistry extensionRegistry;

    public PluginManager(PluginLoader pluginLoader, ExtensionRegistry extensionRegistry) {
        this.pluginLoader = pluginLoader;
        this.extensionRegistry = extensionRegistry;
    }

    public void registerExtensionPoint(ExtensionPoint<?> extensionPoint) {
        extensionPoints.put(extensionPoint.getName(), extensionPoint);
    }

    public Plugin loadPlugin(String pluginPath) {
        try {
            Plugin plugin = pluginLoader.loadPlugin(pluginPath);
            plugins.put(plugin.getId(), plugin);

            // Register plugin extensions
            for (Object extension : plugin.getExtensions()) {
                extensionRegistry.register(extension.getClass(), extension);
            }

            return plugin;
        } catch (Exception e) {
            throw new PluginLoadException("Failed to load plugin from: " + pluginPath, e);
        }
    }

    public void enablePlugin(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin != null && plugin.getStatus() == PluginStatus.DISABLED) {
            plugin.start();
        }
    }

    public void disablePlugin(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin != null && plugin.getStatus() == PluginStatus.ENABLED) {
            plugin.stop();
        }
    }

    public Optional<Plugin> getPlugin(String pluginId) {
        return Optional.ofNullable(plugins.get(pluginId));
    }

    public List<Plugin> getAllPlugins() {
        return new ArrayList<>(plugins.values());
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getExtensions(Class<T> extensionType) {
        return extensionRegistry.getExtensions(extensionType);
    }
}
```

#### 6.2 Create Extension Points

```java
// src/main/java/com/example/mcpserver/extension/point/ToolExtensionPoint.java
package com.example.mcpserver.extension.point;

import com.example.mcpserver.tool.api.McpTool;
import com.example.mcpserver.extension.ExtensionPoint;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ToolExtensionPoint implements ExtensionPoint<McpTool> {
    private final List<McpTool> extensions = new CopyOnWriteArrayList<>();

    @Override
    public String getName() {
        return "tools";
    }

    @Override
    public String getDescription() {
        return "Extension point for MCP tools";
    }

    @Override
    public Class<McpTool> getExtensionType() {
        return McpTool.class;
    }

    @Override
    public List<McpTool> getExtensions() {
        return new ArrayList<>(extensions);
    }

    @Override
    public void addExtension(McpTool extension) {
        if (!extensions.contains(extension)) {
            extensions.add(extension);
        }
    }

    @Override
    public void removeExtension(McpTool extension) {
        extensions.remove(extension);
    }
}
```

#### 6.3 Phase 6 Checklist

- [ ] Create `PluginManager`
- [ ] Create `ExtensionPoint` interface
- [ ] Create `ToolExtensionPoint`
- [ ] Create `InterceptorExtensionPoint`
- [ ] Create `Plugin` interface and implementation
- [ ] Create `PluginLoader`
- [ ] Create `ExtensionRegistry`
- [ ] Create plugin metadata system
- [ ] Test plugin loading and unloading
- [ ] Test extension point registration

### Phase 7: Comprehensive Monitoring and Security (Week 7)

**Goal**: Add comprehensive monitoring, security, and production-ready features.

#### 7.1 Create Security Manager

```java
// src/main/java/com/example/mcpserver/security/SecurityManager.java
package com.example.mcpserver.security;

import com.example.mcpserver.tool.api.*;
import org.springframework.stereotype.Component;

@Component
public class SecurityManager {
    private final AuthenticationProvider authProvider;
    private final AuthorizationProvider authzProvider;
    private final RateLimitingService rateLimitingService;

    public SecurityManager(AuthenticationProvider authProvider,
                          AuthorizationProvider authzProvider,
                          RateLimitingService rateLimitingService) {
        this.authProvider = authProvider;
        this.authzProvider = authzProvider;
        this.rateLimitingService = rateLimitingService;
    }

    public SecurityContext authenticate(String token) {
        return authProvider.authenticate(token);
    }

    public boolean authorize(SecurityContext context, String toolName, String action) {
        return authzProvider.authorize(context, toolName, action);
    }

    public boolean checkRateLimit(String clientId, String toolName) {
        return rateLimitingService.isAllowed(clientId, toolName);
    }

    public void recordAccess(SecurityContext context, String toolName, boolean success) {
        // Record access for audit and rate limiting
        rateLimitingService.recordAccess(context.getClientId(), toolName);
        // Additional audit logging...
    }
}
```

#### 7.2 Create Metrics System

```java
// src/main/java/com/example/mcpserver/infrastructure/metrics/MetricsCollector.java
package com.example.mcpserver.infrastructure.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MetricsCollector {
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Timer> toolTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> toolCounters = new ConcurrentHashMap<>();

    public MetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordToolExecution(String toolName, Duration duration, boolean success) {
        // Record execution time
        Timer timer = toolTimers.computeIfAbsent(toolName,
            name -> Timer.builder("mcp.tool.execution.time")
                .tag("tool", name)
                .register(meterRegistry));
        timer.record(duration);

        // Record execution count
        Counter counter = toolCounters.computeIfAbsent(toolName + "." + success,
            key -> Counter.builder("mcp.tool.execution.count")
                .tag("tool", toolName)
                .tag("success", String.valueOf(success))
                .register(meterRegistry));
        counter.increment();
    }

    public void recordError(String errorType, String toolName) {
        Counter.builder("mcp.tool.errors")
            .tag("type", errorType)
            .tag("tool", toolName)
            .register(meterRegistry)
            .increment();
    }

    public void recordSystemMetric(String name, double value) {
        Gauge.builder("mcp.system." + name)
            .register(meterRegistry, () -> value);
    }
}
```

#### 7.3 Phase 7 Checklist

- [ ] Create `SecurityManager`
- [ ] Implement authentication and authorization
- [ ] Create `MetricsCollector`
- [ ] Implement comprehensive error handling
- [ ] Create audit logging system
- [ ] Implement health checks
- [ ] Add performance monitoring
- [ ] Create security interceptors
- [ ] Test security features
- [ ] Test monitoring and metrics

## Complete Package Structure

```
src/main/java/com/example/mcpserver/
├── core/
│   ├── server/
│   │   └── McpServerConfig.java
│   ├── transport/
│   └── routing/
├── tool/
│   ├── api/
│   │   ├── McpTool.java
│   │   ├── ToolMetadata.java
│   │   ├── ToolResult.java
│   │   ├── ToolContext.java
│   │   └── ValidationResult.java
│   ├── registry/
│   │   ├── ToolRegistry.java
│   │   ├── ToolDefinition.java
│   │   └── ToolRegistryListener.java
│   ├── factory/
│   │   ├── ToolFactory.java
│   │   └── DefaultToolFactory.java
│   ├── execution/
│   │   ├── ToolExecutionEngine.java
│   │   ├── ToolExecutionRequest.java
│   │   └── interceptor/
│   │       ├── ToolInterceptor.java
│   │       ├── LoggingInterceptor.java
│   │       ├── MetricsInterceptor.java
│   │       └── SecurityInterceptor.java
│   ├── validation/
│   │   └── ToolValidator.java
│   ├── discovery/
│   │   ├── ToolDiscoveryService.java
│   │   └── ToolScanner.java
│   └── lifecycle/
│       └── ToolLifecycleManager.java
├── config/
│   ├── ConfigurationManager.java
│   ├── ToolConfiguration.java
│   ├── SecurityConfiguration.java
│   ├── source/
│   │   ├── ConfigurationSource.java
│   │   ├── YamlConfigurationSource.java
│   │   ├── PropertiesConfigurationSource.java
│   │   └── EnvironmentConfigurationSource.java
│   ├── validation/
│   │   └── ConfigurationValidator.java
│   └── cache/
│       └── ConfigurationCache.java
├── security/
│   ├── SecurityManager.java
│   ├── AuthenticationProvider.java
│   ├── AuthorizationProvider.java
│   └── RateLimitingService.java
├── infrastructure/
│   ├── error/
│   │   ├── GlobalErrorHandler.java
│   │   └── ErrorHandler.java
│   ├── logging/
│   │   ├── McpLogger.java
│   │   └── AuditLogger.java
│   ├── metrics/
│   │   └── MetricsCollector.java
│   └── health/
│       └── HealthChecker.java
├── extension/
│   ├── ExtensionPoint.java
│   ├── plugin/
│   │   ├── PluginManager.java
│   │   ├── Plugin.java
│   │   └── PluginLoader.java
│   ├── point/
│   │   ├── ToolExtensionPoint.java
│   │   └── InterceptorExtensionPoint.java
│   └── registry/
│       └── ExtensionRegistry.java
├── tools/
│   ├── builtin/
│   │   ├── GetCurrentTimeTool.java
│   │   └── EchoTool.java
│   └── utility/
└── controller/
    └── McpServerController.java
```

## Testing Strategy

### Unit Testing
```java
// Example unit test for ToolRegistry
@ExtendWith(MockitoExtension.class)
class ToolRegistryTest {

    @Mock
    private ToolRegistryListener listener;

    private ToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        toolRegistry = new ToolRegistry();
        toolRegistry.addListener(listener);
    }

    @Test
    void shouldRegisterTool() {
        // Given
        ToolDefinition definition = createTestToolDefinition();

        // When
        toolRegistry.register(definition);

        // Then
        assertTrue(toolRegistry.isRegistered("test-tool"));
        verify(listener).onToolRegistered(definition);
    }

    @Test
    void shouldFindToolByName() {
        // Given
        ToolDefinition definition = createTestToolDefinition();
        toolRegistry.register(definition);

        // When
        Optional<ToolDefinition> found = toolRegistry.findByName("test-tool");

        // Then
        assertTrue(found.isPresent());
        assertEquals(definition, found.get());
    }
}
```

### Integration Testing
```java
@SpringBootTest
@TestPropertySource(properties = {
    "mcp.tools.discovery.enabled=true",
    "mcp.tools.packages=com.example.mcpserver.tools.builtin"
})
class ToolDiscoveryIntegrationTest {

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private ToolDiscoveryService discoveryService;

    @Test
    void shouldDiscoverBuiltinTools() {
        // When
        discoveryService.discoverTools();

        // Then
        assertTrue(toolRegistry.isRegistered("get_current_time"));
        assertTrue(toolRegistry.isRegistered("echo"));
    }
}
```

## Configuration Examples

### tools.yml
```yaml
tools:
  get_current_time:
    enabled: true
    timeout: PT30S
    properties:
      default_format: "readable"
    security:
      authentication_required: false
      permissions: ["time:read"]
      rate_limiting:
        requests_per_minute: 60

  echo:
    enabled: true
    timeout: PT10S
    properties:
      max_length: 1000
    security:
      authentication_required: false
      permissions: ["echo:execute"]

server:
  discovery:
    enabled: true
    packages:
      - "com.example.mcpserver.tools.builtin"
      - "com.example.mcpserver.tools.custom"

  execution:
    default_timeout: PT30S
    max_concurrent_executions: 100

  security:
    enabled: true
    default_authentication_required: false
```

## Migration Checklist Summary

### Phase 1: Extract Tool Definitions ✓
- [ ] Create tool API interfaces
- [ ] Extract GetCurrentTimeTool
- [ ] Extract EchoTool
- [ ] Create tool metadata system
- [ ] Update tests

### Phase 2: Tool Registry and Factory ✓
- [ ] Create ToolRegistry
- [ ] Create ToolFactory
- [ ] Update McpServerConfig
- [ ] Test tool registration

### Phase 3: Discovery and Lifecycle ✓
- [ ] Create ToolDiscoveryService
- [ ] Create ToolLifecycleManager
- [ ] Implement automatic discovery
- [ ] Test lifecycle management

### Phase 4: Execution Engine ✓
- [ ] Create ToolExecutionEngine
- [ ] Create interceptor system
- [ ] Update McpServerConfig
- [ ] Test execution flow

### Phase 5: Configuration Management ✓
- [ ] Create ConfigurationManager
- [ ] Implement configuration sources
- [ ] Create tool configuration
- [ ] Test configuration loading

### Phase 6: Extension System ✓
- [ ] Create PluginManager
- [ ] Create extension points
- [ ] Test plugin loading
- [ ] Test extension registration

### Phase 7: Monitoring and Security ✓
- [ ] Create SecurityManager
- [ ] Create MetricsCollector
- [ ] Implement comprehensive monitoring
- [ ] Test security features

## Support and Troubleshooting

### Common Issues

1. **Tool Not Found**: Check tool registration in ToolRegistry
2. **Configuration Not Loading**: Verify configuration source priority
3. **Plugin Loading Failed**: Check plugin metadata and dependencies
4. **Security Errors**: Verify authentication and authorization setup

### Debug Tips

1. Enable debug logging: `logging.level.com.example.mcpserver=DEBUG`
2. Check tool registry contents via REST API
3. Monitor metrics for execution patterns
4. Review audit logs for security events

This comprehensive guide provides everything needed to migrate your MCP server to the new architecture systematically and safely.
