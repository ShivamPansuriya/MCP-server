package com.example.mcpserver.tool.api;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

/**
 * Represents the result of tool argument validation.
 * Contains validation status and any error messages.
 */
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    private ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = errors != null ? List.copyOf(errors) : List.of();
        this.warnings = warnings != null ? List.copyOf(warnings) : List.of();
    }

    /**
     * Creates a successful validation result.
     * 
     * @return a successful ValidationResult
     */
    public static ValidationResult success() {
        return new ValidationResult(true, List.of(), List.of());
    }

    /**
     * Creates a successful validation result with warnings.
     * 
     * @param warnings the validation warnings
     * @return a successful ValidationResult with warnings
     */
    public static ValidationResult successWithWarnings(List<String> warnings) {
        return new ValidationResult(true, List.of(), warnings);
    }

    /**
     * Creates a failed validation result with a single error.
     * 
     * @param error the validation error
     * @return a failed ValidationResult
     */
    public static ValidationResult failure(String error) {
        return new ValidationResult(false, List.of(error), List.of());
    }

    /**
     * Creates a failed validation result with multiple errors.
     * 
     * @param errors the validation errors
     * @return a failed ValidationResult
     */
    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors, List.of());
    }

    /**
     * Creates a failed validation result with errors and warnings.
     * 
     * @param errors the validation errors
     * @param warnings the validation warnings
     * @return a failed ValidationResult
     */
    public static ValidationResult failure(List<String> errors, List<String> warnings) {
        return new ValidationResult(false, errors, warnings);
    }

    /**
     * Indicates whether the validation was successful.
     * 
     * @return true if validation passed, false otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns the validation errors.
     * 
     * @return the list of validation errors
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Returns the validation warnings.
     * 
     * @return the list of validation warnings
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Indicates whether there are any warnings.
     * 
     * @return true if there are warnings, false otherwise
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Returns a formatted error message containing all errors.
     * 
     * @return formatted error message, or empty string if no errors
     */
    public String getFormattedErrors() {
        if (errors.isEmpty()) {
            return "";
        }
        return String.join("; ", errors);
    }

    /**
     * Returns a formatted warning message containing all warnings.
     * 
     * @return formatted warning message, or empty string if no warnings
     */
    public String getFormattedWarnings() {
        if (warnings.isEmpty()) {
            return "";
        }
        return String.join("; ", warnings);
    }

    /**
     * Combines this validation result with another.
     * The result is valid only if both results are valid.
     * 
     * @param other the other validation result
     * @return combined validation result
     */
    public ValidationResult combine(ValidationResult other) {
        if (other == null) {
            return this;
        }

        List<String> combinedErrors = new ArrayList<>(this.errors);
        combinedErrors.addAll(other.errors);

        List<String> combinedWarnings = new ArrayList<>(this.warnings);
        combinedWarnings.addAll(other.warnings);

        boolean combinedValid = this.valid && other.valid;

        return new ValidationResult(combinedValid, combinedErrors, combinedWarnings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationResult that = (ValidationResult) o;
        return valid == that.valid &&
               Objects.equals(errors, that.errors) &&
               Objects.equals(warnings, that.warnings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valid, errors, warnings);
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
               "valid=" + valid +
               ", errors=" + errors +
               ", warnings=" + warnings +
               '}';
    }
}
