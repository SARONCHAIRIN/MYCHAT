package com.rindev.chat.exception;

/** The message must be safe for the API client and exclude persistence details. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
