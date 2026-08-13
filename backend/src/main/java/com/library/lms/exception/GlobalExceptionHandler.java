package com.library.lms.exception;

import com.library.lms.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Translates exceptions into the project-wide {@link ErrorResponse} contract.
 *
 * <p><strong>Safe vs. Internal Split:</strong>
 * Known business rules (e.g. {@link ApiException}) are considered "safe" — their messages 
 * are surfaced directly to the client. Unanticipated faults ({@link Exception}) are "internal" — 
 * their stack traces and details are logged with a UUID, while the client receives a generic 
 * message and the UUID to quote for support.</p>
 *
 * <p>Rules.md #4: the caller never sees a stack trace, a file path, or a raw
 * driver error.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Deliberate, safe-to-display errors thrown by our own services. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        log.warn("Business error [{}]: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ErrorResponse.of(ex.getMessage(), ex.getCode(), ex.getStatus().value()));
    }

    /** Bean-validation failures — the only case that returns a per-field map. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
            fields.put(field, error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(ErrorResponse.validation(fields));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("Invalid credentials.", "BAD_CREDENTIALS", 401));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        "You do not have permission to perform this action.", "ACCESS_DENIED", 403));
    }

    /** Anything unanticipated. Full detail to the log, nothing useful to the caller. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("Unhandled exception [traceId={}]", traceId, ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        "An unexpected error occurred. Quote reference " + traceId + " if you contact support.",
                        "INTERNAL_ERROR",
                        500));
    }
}
