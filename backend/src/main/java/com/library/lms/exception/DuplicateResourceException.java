package com.library.lms.exception;

import org.springframework.http.HttpStatus;

/** 409 — a uniqueness constraint would be violated. */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }
}
