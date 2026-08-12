package com.library.lms.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * The single error shape returned by every non-2xx response.
 * Null members are omitted, so {@code fields} appears only on validation failures.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        String code,
        int status,
        Instant timestamp,
        Map<String, String> fields
) {
    public static ErrorResponse of(String message, String code, int status) {
        return new ErrorResponse(message, code, status, Instant.now(), null);
    }

    public static ErrorResponse validation(Map<String, String> fields) {
        return new ErrorResponse(
                "Some fields are invalid. Please correct them and try again.",
                "VALIDATION_FAILED",
                400,
                Instant.now(),
                fields
        );
    }
}
