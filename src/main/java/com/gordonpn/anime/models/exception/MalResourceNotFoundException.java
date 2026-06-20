package com.gordonpn.anime.models.exception;

public class MalResourceNotFoundException extends RuntimeException {
    public MalResourceNotFoundException(String message) {
        super(message);
    }

    public MalResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
