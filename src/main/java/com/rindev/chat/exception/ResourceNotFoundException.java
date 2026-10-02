package com.rindev.chat.exception;

/** The message must be safe for the API client. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
