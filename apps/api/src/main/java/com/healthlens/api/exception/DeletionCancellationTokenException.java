package com.healthlens.api.exception;

public class DeletionCancellationTokenException extends RuntimeException {
    public DeletionCancellationTokenException(String message) {
        super(message);
    }
}
