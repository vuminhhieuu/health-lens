package com.healthlens.api.exception;

public class AccountPendingDeletionException extends RuntimeException {
    public AccountPendingDeletionException() {
        super("Tai khoan dang cho xoa");
    }
}