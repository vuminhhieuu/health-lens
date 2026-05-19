package com.healthlens.api.exception;

public class DeletionCancellationConflictException extends RuntimeException {
    public DeletionCancellationConflictException(String message) {
        super(message);
    }
}
