package com.healthlens.api.exception;

public class AccountLockedException extends RuntimeException {

    private final long retryAfterSeconds;

    public AccountLockedException(String message) {
        super(message);
        this.retryAfterSeconds = 900;
    }

    public AccountLockedException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
