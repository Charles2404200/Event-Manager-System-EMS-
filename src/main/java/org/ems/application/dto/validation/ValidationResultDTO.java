package org.ems.application.dto.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for validation results
 * Contains validation status and error messages if any
 * @author <your group number>
 */
public class ValidationResultDTO {
    private final boolean valid;
    private final List<String> errors;

    private ValidationResultDTO(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    public static ValidationResultDTO success() {
        return new ValidationResultDTO(true, new ArrayList<>());
    }

    public static ValidationResultDTO failure(String error) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return new ValidationResultDTO(false, errors);
    }

    public static ValidationResultDTO failure(List<String> errors) {
        return new ValidationResultDTO(false, errors);
    }

    // Getters
    public boolean isValid() { return valid; }
    public List<String> getErrors() { return new ArrayList<>(errors); }
    public String getFirstError() { return !errors.isEmpty() ? errors.get(0) : null; }
    public String getAllErrorsAsString() { return String.join("\n", errors); }

    @Override
    public String toString() {
        return "ValidationResultDTO{" +
                "valid=" + valid +
                ", errors=" + errors +
                '}';
    }
}

