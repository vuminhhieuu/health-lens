package com.healthlens.api.exception;

public class AccountPendingDeletionException extends RuntimeException {
    public AccountPendingDeletionException() {
        super("Tài khoản đang chờ xóa");
    }
}
