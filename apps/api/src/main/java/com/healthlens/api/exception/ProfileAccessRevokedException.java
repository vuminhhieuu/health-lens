package com.healthlens.api.exception;

import org.springframework.security.access.AccessDeniedException;

/**
 * Signals that the caller's access to a shared profile is no longer valid (e.g. share revoked).
 * Returned to clients with a stable machine-readable error code for safe UX decisions.
 */
public class ProfileAccessRevokedException extends AccessDeniedException {
    public static final String ERROR_CODE = "PROFILE_ACCESS_REVOKED";

    public ProfileAccessRevokedException(String msg) {
        super(msg);
    }
}

