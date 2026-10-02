package com.supermarket.exception;

import org.springframework.http.HttpStatus;

/** 403 - authenticated but not allowed (RBAC / branch-level authorization). */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
