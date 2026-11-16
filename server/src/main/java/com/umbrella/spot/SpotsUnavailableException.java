package com.umbrella.spot;

public class SpotsUnavailableException extends RuntimeException {

    public SpotsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
