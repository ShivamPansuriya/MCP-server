package com.example.mcpserver.tool.api;

import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.Collections;

/**
 * Comprehensive metadata for MCP tools.
 * Contains all information needed for tool registration, discovery, and execution.
 */
public class ToolMetadata {
    private final String name;
    private final String description;
    private final String version;
    private final String category;
    private final Set<String> tags;
    private final String schema;
    private final Map<String, Object> configuration;

    private ToolMetadata(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.version = builder.version;
        this.category = builder.category;
        this.tags = Collections.unmodifiableSet(builder.tags);
        this.schema = builder.schema;
        this.configuration = Collections.unmodifiableMap(builder.configuration);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public String getCategory() {
        return category;
    }

    public Set<String> getTags() {
        return tags;
    }

    public String getSchema() {
        return schema;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolMetadata that = (ToolMetadata) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version);
    }

    @Override
    public String toString() {
        return "ToolMetadata{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", version='" + version + '\'' +
               ", category='" + category + '\'' +
               ", tags=" + tags +
               '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private String version = "1.0.0";
        private String category = "general";
        private Set<String> tags = Set.of();
        private String schema = "{}";
        private Map<String, Object> configuration = Map.of();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder tags(Set<String> tags) {
            this.tags = Set.copyOf(tags);
            return this;
        }

        public Builder schema(String schema) {
            this.schema = schema;
            return this;
        }

        public Builder configuration(Map<String, Object> configuration) {
            this.configuration = Map.copyOf(configuration);
            return this;
        }

        public ToolMetadata build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Tool name cannot be null or empty");
            }
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("Tool description cannot be null or empty");
            }
            return new ToolMetadata(this);
        }
    }
}
