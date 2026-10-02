package com.rindev.chat.exception;

/** The message must be safe for the API client and exclude submitted credentials. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        this("Authentication required");
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}
