package com.healthlens.api.exception;

public class WeakPasswordException extends RuntimeException {

    private final String field;

    public WeakPasswordException(String message) {
        this(message, "password");
    }

    public WeakPasswordException(String message, String field) {
        super(message);
        this.field = field != null && !field.isBlank() ? field : "password";
    }

    public String getField() {
        return field;
    }
}
