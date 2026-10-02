package com.supermarket.exception;

import org.springframework.http.HttpStatus;

/** 404 - requested entity does not exist. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
