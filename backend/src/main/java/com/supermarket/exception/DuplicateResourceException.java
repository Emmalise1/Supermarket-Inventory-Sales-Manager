package com.supermarket.exception;

import org.springframework.http.HttpStatus;

/** 409 - unique constraint violated (duplicate barcode, email, ...). */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
