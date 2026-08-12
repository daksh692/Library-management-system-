package com.library.lms.exception;

import org.springframework.http.HttpStatus;

/** 409 — the request is well-formed but violates a library rule. */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message, String code) {
        super(message, HttpStatus.CONFLICT, code);
    }
}
