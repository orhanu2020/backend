package com.socialmedia.exception;

import org.springframework.http.HttpStatus;

public class TwitterApiException extends RuntimeException {
    private final HttpStatus statusCode;

    public TwitterApiException(String message, HttpStatus statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }
}

