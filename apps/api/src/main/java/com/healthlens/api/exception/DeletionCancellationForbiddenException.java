package com.healthlens.api.exception;

public class DeletionCancellationForbiddenException extends RuntimeException {
    public DeletionCancellationForbiddenException(String message) {
        super(message);
    }
}
