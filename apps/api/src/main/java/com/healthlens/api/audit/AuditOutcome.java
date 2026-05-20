package com.healthlens.api.audit;

/**
 * Processing outcome for rows in {@code audit_logs}.
 * Most entries are written only after a successful operation; explicit failure actions are logged separately.
 */
public final class AuditOutcome {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";

    private AuditOutcome() {}

    public static String fromAction(String action) {
        if (action == null || action.isBlank()) {
            return SUCCESS;
        }
        if (AuditActions.LOGIN_FAILED.equals(action)
                || action.endsWith("_FAILED")
                || action.contains("_FAILED_")
                || action.endsWith("_DEAD_LETTERED")) {
            return FAILURE;
        }
        return SUCCESS;
    }
}
