package com.example.mcpserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing incident storage and operations.
 * Provides thread-safe in-memory storage for incident management system.
 */
@Service
public class IncidentStorageService {

    private static final Logger logger = LoggerFactory.getLogger(IncidentStorageService.class);

    // Thread-safe storage for incidents
    private final Map<String, Map<String, Object>> incidents = new ConcurrentHashMap<>();
    
    // Constants for incident management
    public static final Set<String> VALID_PRIORITIES = Set.of("Low", "Medium", "High", "Critical");
    public static final Set<String> VALID_STATUSES = Set.of("Open", "In Progress", "Resolved", "Closed");

    /**
     * Generates a unique incident ID in the format INC-{8-char-hex}.
     *
     * @return unique incident ID
     */
    public String generateIncidentId() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String incidentId = "INC-" + uuid.substring(0, 8).toUpperCase();
        logger.debug("Generated new incident ID: {}", incidentId);
        return incidentId;
    }
    
    /**
     * Creates a new incident with the provided details.
     * 
     * @param title the incident title (required)
     * @param requester the person raising the incident (required)
     * @param description the incident description (optional)
     * @param priority the incident priority (defaults to "Medium")
     * @return the created incident data
     */
    public Map<String, Object> createIncident(String title, String requester, String description, String priority) {
        logger.info("Creating new incident with title: '{}', requester: '{}', priority: '{}'", title, requester, priority);

        String incidentId = generateIncidentId();
        String createdAt = Instant.now().toString();

        Map<String, Object> incident = new HashMap<>();
        incident.put("incident_id", incidentId);
        incident.put("title", title);
        incident.put("requester", requester);
        incident.put("description", description != null ? description : "");
        incident.put("priority", priority != null ? priority : "Medium");
        incident.put("status", "Open");
        incident.put("created_at", createdAt);

        incidents.put(incidentId, incident);
        logger.info("Successfully created incident: {} (total incidents: {})", incidentId, incidents.size());
        logger.debug("Incident details: {}", incident);

        return new HashMap<>(incident); // Return a copy
    }

    /**
     * Creates a new incident with flexible field data.
     * This method accepts any field data and creates an incident with those fields.
     *
     * @param fieldData map of field names to values
     * @return the created incident data
     */
    public Map<String, Object> createIncidentWithFields(Map<String, Object> fieldData) {
        logger.info("Creating new incident with flexible field data: {}", fieldData.keySet());

        String incidentId = generateIncidentId();
        String createdAt = Instant.now().toString();

        Map<String, Object> incident = new HashMap<>();
        incident.put("incident_id", incidentId);
        incident.put("created_at", createdAt);
        incident.put("status", "Open"); // Default status

        // Add all provided field data
        if (fieldData != null) {
            for (Map.Entry<String, Object> entry : fieldData.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldValue = entry.getValue();
                incident.put(fieldName, fieldValue);
                logger.debug("Added field '{}': '{}'", fieldName, fieldValue);
            }
        }

        incidents.put(incidentId, incident);
        logger.info("Successfully created incident: {} with {} fields (total incidents: {})",
                   incidentId, fieldData != null ? fieldData.size() : 0, incidents.size());
        logger.debug("Incident details: {}", incident);

        return new HashMap<>(incident); // Return a copy
    }

    /**
     * Retrieves an incident by ID.
     *
     * @param incidentId the incident ID
     * @return the incident data, or null if not found
     */
    public Map<String, Object> getIncident(String incidentId) {
        logger.debug("Retrieving incident: {}", incidentId);
        Map<String, Object> incident = incidents.get(incidentId);
        if (incident != null) {
            logger.debug("Found incident: {}", incidentId);
            return new HashMap<>(incident);
        } else {
            logger.debug("Incident not found: {}", incidentId);
            return null;
        }
    }
    
    /**
     * Updates an existing incident with the provided field updates.
     * 
     * @param incidentId the incident ID
     * @param updates map of field names to new values
     * @return the updated incident data, or null if incident not found
     */
    public Map<String, Object> updateIncident(String incidentId, Map<String, Object> updates) {
        logger.info("Updating incident: {} with {} fields", incidentId, updates.size());
        logger.debug("Update fields: {}", updates);

        Map<String, Object> incident = incidents.get(incidentId);
        if (incident == null) {
            logger.warn("Cannot update incident: {} not found", incidentId);
            return null;
        }

        // Apply updates for valid fields only
        int updatedFieldCount = 0;
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            String oldValue = (String) incident.get(field);
            incident.put(field, value.toString());
            logger.debug("Updated field '{}': '{}' -> '{}'", field, oldValue, value.toString());
            updatedFieldCount++;
        }

        logger.info("Successfully updated incident: {} ({} fields changed)", incidentId, updatedFieldCount);
        return new HashMap<>(incident); // Return a copy
    }
    
    /**
     * Checks if an incident exists.
     *
     * @param incidentId the incident ID
     * @return true if the incident exists, false otherwise
     */
    public boolean incidentExists(String incidentId) {
        boolean exists = incidents.containsKey(incidentId);
        logger.debug("Incident existence check for '{}': {}", incidentId, exists);
        return exists;
    }
    
    /**
     * Gets all incidents (for debugging/admin purposes).
     * 
     * @return map of all incidents
     */
    public Map<String, Map<String, Object>> getAllIncidents() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : incidents.entrySet()) {
            result.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return result;
    }
    
    /**
     * Validates if a priority value is valid.
     * 
     * @param priority the priority to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidPriority(String priority) {
        return priority != null && VALID_PRIORITIES.contains(priority);
    }
    
    /**
     * Validates if a status value is valid.
     *
     * @param status the status to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidStatus(String status) {
        return status != null && VALID_STATUSES.contains(status);
    }

    /**
     * Deletes an incident by ID.
     *
     * @param incidentId the incident ID to delete
     * @return the deleted incident data, or null if incident not found
     */
    public Map<String, Object> deleteIncident(String incidentId) {
        logger.info("Attempting to delete incident: {}", incidentId);

        Map<String, Object> incident = incidents.get(incidentId);
        if (incident == null) {
            logger.warn("Cannot delete incident: {} not found", incidentId);
            return null;
        }

        // Create a copy of the incident before deletion for return
        Map<String, Object> deletedIncident = new HashMap<>(incident);

        // Remove the incident from storage
        incidents.remove(incidentId);

        logger.info("Successfully deleted incident: {} (remaining incidents: {})", incidentId, incidents.size());
        logger.debug("Deleted incident details: {}", deletedIncident);

        return deletedIncident;
    }
}
