package com.rindev.chat.exception;

/** The message must be safe for the API client. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException() {
        this("Access denied");
    }

    public ForbiddenException(String message) {
        super(message);
    }
}
