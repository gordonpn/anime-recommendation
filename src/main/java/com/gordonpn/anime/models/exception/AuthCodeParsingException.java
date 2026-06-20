package com.gordonpn.anime.models.exception;

public class AuthCodeParsingException extends RuntimeException {
    public AuthCodeParsingException(String message) {
        super(message);
    }

    public AuthCodeParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
