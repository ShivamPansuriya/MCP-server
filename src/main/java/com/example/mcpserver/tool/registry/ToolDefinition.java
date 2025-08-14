package com.example.mcpserver.tool.registry;

import com.example.mcpserver.tool.api.McpTool;
import com.example.mcpserver.tool.api.ToolMetadata;

import java.time.Instant;
import java.util.Objects;

/**
 * Encapsulates tool metadata and configuration for registry management.
 * Contains tool metadata, class information, and runtime status.
 */
public class ToolDefinition {
    private final ToolMetadata metadata;
    private final Class<? extends McpTool> toolClass;
    private volatile ToolStatus status;
    private final Instant registrationTime;
    private volatile Instant lastAccessTime;
    private volatile long executionCount;

    private ToolDefinition(Builder builder) {
        this.metadata = builder.metadata;
        this.toolClass = builder.toolClass;
        this.status = builder.status;
        this.registrationTime = builder.registrationTime != null ? builder.registrationTime : Instant.now();
        this.lastAccessTime = this.registrationTime;
        this.executionCount = 0;
    }

    /**
     * Returns the tool metadata.
     * 
     * @return the tool metadata
     */
    public ToolMetadata getMetadata() {
        return metadata;
    }

    /**
     * Returns the tool class.
     * 
     * @return the tool class
     */
    public Class<? extends McpTool> getToolClass() {
        return toolClass;
    }

    /**
     * Returns the current tool status.
     * 
     * @return the tool status
     */
    public ToolStatus getStatus() {
        return status;
    }

    /**
     * Sets the tool status.
     * 
     * @param status the new status
     */
    public void setStatus(ToolStatus status) {
        this.status = status;
    }

    /**
     * Returns the registration time.
     * 
     * @return the registration time
     */
    public Instant getRegistrationTime() {
        return registrationTime;
    }

    /**
     * Returns the last access time.
     * 
     * @return the last access time
     */
    public Instant getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * Updates the last access time to now.
     */
    public void updateLastAccessTime() {
        this.lastAccessTime = Instant.now();
    }

    /**
     * Returns the execution count.
     * 
     * @return the execution count
     */
    public long getExecutionCount() {
        return executionCount;
    }

    /**
     * Increments the execution count.
     */
    public void incrementExecutionCount() {
        this.executionCount++;
    }

    /**
     * Checks if the tool is currently enabled.
     * 
     * @return true if the tool is enabled, false otherwise
     */
    public boolean isEnabled() {
        return status == ToolStatus.ENABLED;
    }

    /**
     * Checks if the tool is currently disabled.
     * 
     * @return true if the tool is disabled, false otherwise
     */
    public boolean isDisabled() {
        return status == ToolStatus.DISABLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolDefinition that = (ToolDefinition) o;
        return Objects.equals(metadata.getName(), that.metadata.getName()) &&
               Objects.equals(toolClass, that.toolClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata.getName(), toolClass);
    }

    @Override
    public String toString() {
        return "ToolDefinition{" +
               "name='" + metadata.getName() + '\'' +
               ", class=" + toolClass.getSimpleName() +
               ", status=" + status +
               ", registrationTime=" + registrationTime +
               ", executionCount=" + executionCount +
               '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ToolMetadata metadata;
        private Class<? extends McpTool> toolClass;
        private ToolStatus status = ToolStatus.ENABLED;
        private Instant registrationTime;

        public Builder metadata(ToolMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder toolClass(Class<? extends McpTool> toolClass) {
            this.toolClass = toolClass;
            return this;
        }

        public Builder status(ToolStatus status) {
            this.status = status;
            return this;
        }

        public Builder registrationTime(Instant registrationTime) {
            this.registrationTime = registrationTime;
            return this;
        }

        public ToolDefinition build() {
            if (metadata == null) {
                throw new IllegalArgumentException("Tool metadata cannot be null");
            }
            if (toolClass == null) {
                throw new IllegalArgumentException("Tool class cannot be null");
            }
            return new ToolDefinition(this);
        }
    }
}
