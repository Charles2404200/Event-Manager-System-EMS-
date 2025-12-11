package org.ems.application.service;

/**
 * TicketValidationService - Validates user inputs and business rules
 * Single Responsibility: Input validation only
 *
 * @author EMS Team
 */
public class TicketValidationService {

    /**
     * Validate template creation form inputs
     */
    public ValidationResult validateTemplateCreation(String eventSelection, String priceInput, String typeSelection) {
        if (eventSelection == null || eventSelection.isEmpty()) {
            return new ValidationResult(false, "Please select an Event");
        }

        if (typeSelection == null || typeSelection.isEmpty()) {
            return new ValidationResult(false, "Please select a Ticket Type");
        }

        if (priceInput == null || priceInput.trim().isEmpty()) {
            return new ValidationResult(false, "Please enter price");
        }

        // Validate price format
        try {
            double price = Double.parseDouble(priceInput.trim());
            if (price < 0) {
                return new ValidationResult(false, "Price cannot be negative");
            }
            if (price > 999999.99) {
                return new ValidationResult(false, "Price exceeds maximum limit");
            }
        } catch (NumberFormatException ex) {
            return new ValidationResult(false, "Invalid price format. Please enter a valid number");
        }

        return new ValidationResult(true, "Validation passed");
    }

    /**
     * Validate ticket assignment form inputs
     */
    public ValidationResult validateTicketAssignment(String attendeeSelection, String templateSelection) {
        if (attendeeSelection == null || attendeeSelection.isEmpty()) {
            return new ValidationResult(false, "Please select an Attendee");
        }

        if (templateSelection == null || templateSelection.isEmpty()) {
            return new ValidationResult(false, "Please select a Ticket Template");
        }

        return new ValidationResult(true, "Validation passed");
    }

    /**
     * Result class for validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}

