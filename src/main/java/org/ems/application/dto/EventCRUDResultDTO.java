package org.ems.application.dto;

/**
 * EventCRUDResultDTO - Result DTO for CRUD operations
 * Wraps success/failure status and message for UI feedback
 *
 * @author EMS Team
 */
public class EventCRUDResultDTO {
    public boolean success;
    public String message;
    public String type;  // "CREATE", "UPDATE", "DELETE"
    public Object data;  // optional: additional data (e.g., created event ID)

    public EventCRUDResultDTO(boolean success, String message, String type) {
        this(success, message, type, null);
    }

    public EventCRUDResultDTO(boolean success, String message, String type, Object data) {
        this.success = success;
        this.message = message;
        this.type = type;
        this.data = data;
    }

    public static EventCRUDResultDTO success(String message, String type) {
        return new EventCRUDResultDTO(true, message, type);
    }

    public static EventCRUDResultDTO error(String message, String type) {
        return new EventCRUDResultDTO(false, message, type);
    }

    @Override
    public String toString() {
        return "EventCRUDResultDTO{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}

