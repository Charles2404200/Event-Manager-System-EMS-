package org.ems.application.service.event;

/**
 * EventValidationService - Validates event form data and business rules
 * Single Responsibility: Validation logic only (pure functions, no dependencies)
 *
 * @author EMS Team
 */
public class EventValidationService {

    /**
     * Validate event form data
     */
    public ValidationResult validate(String name, String location,
                                     java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (name == null || name.isBlank()) {
            return ValidationResult.error("Event name is required");
        }

        if (location == null || location.isBlank()) {
            return ValidationResult.error("Location is required");
        }

        if (startDate == null) {
            return ValidationResult.error("Start date is required");
        }

        if (endDate == null) {
            return ValidationResult.error("End date is required");
        }

        if (endDate.isBefore(startDate)) {
            return ValidationResult.error("End date must be after or equal to start date");
        }

        return ValidationResult.success("Validation passed");
    }

    /**
     * Validation result wrapper
     */
    public static class ValidationResult {
        public final boolean valid;
        public final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success(String message) {
            return new ValidationResult(true, message);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }
}

