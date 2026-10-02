package com.rindev.chat.exception;

/** The message must be safe for the API client and exclude submitted secrets. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
