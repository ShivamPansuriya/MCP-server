package com.example.mcpserver.tool.registry;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Thread-safe registry for managing MCP tools.
 * Provides registration, lookup, and lifecycle management for tools.
 */
@Component
public class ToolRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);
    
    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> categories = new ConcurrentHashMap<>();
    private final List<ToolRegistryListener> listeners = new CopyOnWriteArrayList<>();
    
    /**
     * Registers a tool definition in the registry.
     * 
     * @param definition the tool definition to register
     * @throws IllegalArgumentException if definition is null or tool name already exists
     */
    public void register(ToolDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Tool definition cannot be null");
        }
        
        String name = definition.getMetadata().getName();
        if (tools.containsKey(name)) {
            logger.warn("Tool '{}' is already registered, replacing existing definition", name);
        }
        
        ToolStatus originalStatus = definition.getStatus();
        definition.setStatus(ToolStatus.REGISTERING);
        tools.put(name, definition);

        String category = definition.getMetadata().getCategory();
        categories.computeIfAbsent(category, k -> ConcurrentHashMap.newKeySet()).add(name);

        // Restore original status or set to ENABLED if it was REGISTERING
        definition.setStatus(originalStatus == ToolStatus.REGISTERING ? ToolStatus.ENABLED : originalStatus);
        
        logger.info("Registered tool: {} (category: {})", name, category);
        notifyListeners(listener -> listener.onToolRegistered(definition));
    }
    
    /**
     * Unregisters a tool from the registry.
     * 
     * @param name the name of the tool to unregister
     * @return true if the tool was unregistered, false if it wasn't found
     */
    public boolean unregister(String name) {
        ToolDefinition definition = tools.get(name);
        if (definition == null) {
            return false;
        }
        
        definition.setStatus(ToolStatus.UNREGISTERING);
        tools.remove(name);
        
        String category = definition.getMetadata().getCategory();
        Set<String> categoryTools = categories.get(category);
        if (categoryTools != null) {
            categoryTools.remove(name);
            if (categoryTools.isEmpty()) {
                categories.remove(category);
            }
        }
        
        logger.info("Unregistered tool: {}", name);
        notifyListeners(listener -> listener.onToolUnregistered(definition));
        return true;
    }
    
    /**
     * Finds a tool definition by name.
     * 
     * @param name the tool name
     * @return the tool definition if found
     */
    public Optional<ToolDefinition> findByName(String name) {
        ToolDefinition definition = tools.get(name);
        if (definition != null) {
            definition.updateLastAccessTime();
        }
        return Optional.ofNullable(definition);
    }
    
    /**
     * Returns all registered tools.
     * 
     * @return list of all tool definitions
     */
    public List<ToolDefinition> getAllTools() {
        return new ArrayList<>(tools.values());
    }
    
    /**
     * Returns all enabled tools.
     * 
     * @return list of enabled tool definitions
     */
    public List<ToolDefinition> getEnabledTools() {
        return tools.values().stream()
            .filter(ToolDefinition::isEnabled)
            .toList();
    }
    
    /**
     * Finds tools by category.
     * 
     * @param category the category name
     * @return list of tools in the specified category
     */
    public List<ToolDefinition> findByCategory(String category) {
        return categories.getOrDefault(category, Collections.emptySet())
            .stream()
            .map(tools::get)
            .filter(Objects::nonNull)
            .toList();
    }
    
    /**
     * Checks if a tool is registered.
     * 
     * @param name the tool name
     * @return true if the tool is registered, false otherwise
     */
    public boolean isRegistered(String name) {
        return tools.containsKey(name);
    }
    
    /**
     * Returns all registered categories.
     * 
     * @return set of category names
     */
    public Set<String> getCategories() {
        return new HashSet<>(categories.keySet());
    }
    
    /**
     * Returns the number of registered tools.
     * 
     * @return the tool count
     */
    public int getToolCount() {
        return tools.size();
    }
    
    /**
     * Returns the number of enabled tools.
     * 
     * @return the enabled tool count
     */
    public int getEnabledToolCount() {
        return (int) tools.values().stream()
            .filter(ToolDefinition::isEnabled)
            .count();
    }
    
    /**
     * Adds a registry listener.
     * 
     * @param listener the listener to add
     */
    public void addListener(ToolRegistryListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes a registry listener.
     * 
     * @param listener the listener to remove
     */
    public void removeListener(ToolRegistryListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Clears all registered tools.
     * This method should be used with caution.
     */
    public void clear() {
        logger.warn("Clearing all registered tools");
        tools.clear();
        categories.clear();
        notifyListeners(listener -> listener.onRegistryCleared());
    }
    
    private void notifyListeners(Consumer<ToolRegistryListener> action) {
        for (ToolRegistryListener listener : listeners) {
            try {
                action.accept(listener);
            } catch (Exception e) {
                logger.error("Error notifying registry listener: {}", e.getMessage(), e);
            }
        }
    }
}
