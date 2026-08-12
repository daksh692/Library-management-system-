package com.library.lms.exception;

import org.springframework.http.HttpStatus;

/**
 * Base for all deliberately-thrown API errors.
 *
 * <p>Anything extending this is considered a <em>safe</em> message: the text is
 * shown to the caller verbatim. Anything that does not extend this is treated as
 * an internal fault and replaced with a generic message.</p>
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }

    public String getCode() { return code; }
}
