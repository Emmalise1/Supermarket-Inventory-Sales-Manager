package com.supermarket.exception;

import org.springframework.http.HttpStatus;

/** 409 - business rule violated (e.g. negative stock would result). */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
