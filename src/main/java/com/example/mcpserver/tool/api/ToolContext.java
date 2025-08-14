package com.example.mcpserver.tool.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides execution context information to tools during execution.
 * Contains session information, request details, and client metadata.
 */
public class ToolContext {
    private final String sessionId;
    private final String requestId;
    private final String clientId;
    private final Map<String, Object> clientInfo;
    private final Instant executionStartTime;
    private final Map<String, Object> properties;

    private ToolContext(Builder builder) {
        this.sessionId = builder.sessionId;
        this.requestId = builder.requestId;
        this.clientId = builder.clientId;
        this.clientInfo = builder.clientInfo != null ? Map.copyOf(builder.clientInfo) : Map.of();
        this.executionStartTime = builder.executionStartTime != null ? builder.executionStartTime : Instant.now();
        this.properties = builder.properties != null ? Map.copyOf(builder.properties) : Map.of();
    }

    /**
     * Returns the session ID for this execution context.
     * 
     * @return the session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the unique request ID for this execution.
     * 
     * @return the request ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Returns the client ID if available.
     * 
     * @return the client ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Returns client information metadata.
     * 
     * @return the client information map
     */
    public Map<String, Object> getClientInfo() {
        return clientInfo;
    }

    /**
     * Returns the execution start time.
     * 
     * @return the execution start time
     */
    public Instant getExecutionStartTime() {
        return executionStartTime;
    }

    /**
     * Returns additional context properties.
     * 
     * @return the properties map
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Gets a property value by key.
     * 
     * @param key the property key
     * @param type the expected type
     * @param <T> the type parameter
     * @return the property value if present and of correct type
     */
    public <T> Optional<T> getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * Gets a string property value.
     * 
     * @param key the property key
     * @return the string property value if present
     */
    public Optional<String> getStringProperty(String key) {
        return getProperty(key, String.class);
    }

    /**
     * Calculates the execution duration from start time to now.
     * 
     * @return the execution duration in milliseconds
     */
    public long getExecutionDurationMs() {
        return Instant.now().toEpochMilli() - executionStartTime.toEpochMilli();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolContext that = (ToolContext) o;
        return Objects.equals(sessionId, that.sessionId) &&
               Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, requestId);
    }

    @Override
    public String toString() {
        return "ToolContext{" +
               "sessionId='" + sessionId + '\'' +
               ", requestId='" + requestId + '\'' +
               ", clientId='" + clientId + '\'' +
               ", executionStartTime=" + executionStartTime +
               '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sessionId;
        private String requestId;
        private String clientId;
        private Map<String, Object> clientInfo;
        private Instant executionStartTime;
        private Map<String, Object> properties;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientInfo(Map<String, Object> clientInfo) {
            this.clientInfo = clientInfo;
            return this;
        }

        public Builder executionStartTime(Instant executionStartTime) {
            this.executionStartTime = executionStartTime;
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        public ToolContext build() {
            return new ToolContext(this);
        }
    }
}
