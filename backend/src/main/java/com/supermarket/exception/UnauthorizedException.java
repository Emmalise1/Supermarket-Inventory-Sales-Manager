package com.supermarket.exception;

import org.springframework.http.HttpStatus;

/** 401 - not authenticated. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
