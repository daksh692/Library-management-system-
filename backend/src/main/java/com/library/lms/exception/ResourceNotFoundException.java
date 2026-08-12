package com.library.lms.exception;

import org.springframework.http.HttpStatus;

/** 404 — the requested entity does not exist or is soft-deleted. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, String identifier) {
        super(resource + " not found: " + identifier,
              HttpStatus.NOT_FOUND,
              resource.toUpperCase().replace(' ', '_') + "_NOT_FOUND");
    }
}
