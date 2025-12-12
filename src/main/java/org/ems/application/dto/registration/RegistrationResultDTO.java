package org.ems.application.dto.registration;

/**
 * DTO for registration operation results
 * Contains status, messages, and operation details
 * Implements Data Transfer Object pattern
 *
 * @author <your group number>
 */
public class RegistrationResultDTO {
    private final boolean success;
    private final String message;
    private final String operationType; // EVENT_REGISTRATION, SESSION_REGISTRATION, UNREGISTRATION
    private final String attendeeId;
    private final String targetId; // eventId or sessionId
    private final String detailedError;
    private final long timestamp;

    public RegistrationResultDTO(boolean success, String message, String operationType,
                                String attendeeId, String targetId, String detailedError) {
        this.success = success;
        this.message = message;
        this.operationType = operationType;
        this.attendeeId = attendeeId;
        this.targetId = targetId;
        this.detailedError = detailedError;
        this.timestamp = System.currentTimeMillis();
    }

    public RegistrationResultDTO(boolean success, String message, String operationType,
                                String attendeeId, String targetId) {
        this(success, message, operationType, attendeeId, targetId, null);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getOperationType() { return operationType; }
    public String getAttendeeId() { return attendeeId; }
    public String getTargetId() { return targetId; }
    public String getDetailedError() { return detailedError; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "RegistrationResultDTO{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", operationType='" + operationType + '\'' +
                ", attendeeId='" + attendeeId + '\'' +
                ", targetId='" + targetId + '\'' +
                '}';
    }
}

