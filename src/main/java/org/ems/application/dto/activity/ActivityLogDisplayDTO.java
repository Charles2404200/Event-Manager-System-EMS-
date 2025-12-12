package org.ems.application.dto.activity;

/**
 * DTO for displaying activity log information in table
 * Immutable representation of activity log data for UI layer
 * @author <your group number>
 */
public class ActivityLogDisplayDTO {
    private final String timestamp;
    private final String userId;
    private final String action;
    private final String resource;
    private final String description;

    public ActivityLogDisplayDTO(String timestamp, String userId, String action, String resource, String description) {
        this.timestamp = timestamp;
        this.userId = userId;
        this.action = action;
        this.resource = resource;
        this.description = description;
    }

    // Getters
    public String getTimestamp() { return timestamp; }
    public String getUserId() { return userId; }
    public String getAction() { return action; }
    public String getResource() { return resource; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "ActivityLogDisplayDTO{" +
                "timestamp='" + timestamp + '\'' +
                ", userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}

