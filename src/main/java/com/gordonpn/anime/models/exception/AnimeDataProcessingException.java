package com.gordonpn.anime.models.exception;

public class AnimeDataProcessingException extends RuntimeException {
    public AnimeDataProcessingException(String message) {
        super(message);
    }

    public AnimeDataProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
