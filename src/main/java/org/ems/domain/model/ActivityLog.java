package org.ems.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ActivityLog {

    private UUID id;
    private String userId;
    private String action;
    private String resource;
    private String description;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String userAgent;

    public ActivityLog() {
        this.id = UUID.randomUUID();
        this.timestamp = LocalDateTime.now();
    }

    public ActivityLog(String userId, String action, String resource, String description) {
        this();
        this.userId = userId;
        this.action = action;
        this.resource = resource;
        this.description = description;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    @Override
    public String toString() {
        return "ActivityLog{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                ", resource='" + resource + '\'' +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

