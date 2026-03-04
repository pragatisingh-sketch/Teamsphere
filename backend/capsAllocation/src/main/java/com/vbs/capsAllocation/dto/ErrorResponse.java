package com.vbs.capsAllocation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Error response structure for validation and other errors
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String status;
    private int code;
    private String message;
    private List<FieldError> errors;

    /**
     * Individual field error
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
    }

    /**
     * Create error response with field errors
     */
    public static ErrorResponse validationError(String message, List<FieldError> errors) {
        return new ErrorResponse("error", 400, message, errors);
    }

    /**
     * Create simple error response
     */
    public static ErrorResponse error(String message, int code) {
        return new ErrorResponse("error", code, message, null);
    }
}
