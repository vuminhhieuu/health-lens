package com.healthlens.api.audit;

public final class AuditActions {
    private AuditActions() {}

    public static final String UPDATE_REFERENCE_METRIC_DISPLAY = "UPDATE_REFERENCE_METRIC_DISPLAY";
    public static final String CREATE_REFERENCE_METRIC = "CREATE_REFERENCE_METRIC";
    public static final String UPDATE_REFERENCE_METRIC = "UPDATE_REFERENCE_METRIC";
    public static final String DEACTIVATE_REFERENCE_METRIC = "DEACTIVATE_REFERENCE_METRIC";
    public static final String REACTIVATE_REFERENCE_METRIC = "REACTIVATE_REFERENCE_METRIC";
    public static final String SUBMIT_REFERENCE_CHANGE_SET = "SUBMIT_REFERENCE_CHANGE_SET";
    public static final String PUBLISH_CHANGE_SET = "PUBLISH_CHANGE_SET";
    public static final String APPROVE_CHANGE_SET = "APPROVE_CHANGE_SET";
    public static final String REJECT_CHANGE_SET = "REJECT_CHANGE_SET";
    public static final String CONFIRM_REFERENCE_IMPORT = "CONFIRM_REFERENCE_IMPORT";

    public static final String CREATE_HEALTH_RECORD = "CREATE_HEALTH_RECORD";
    public static final String CONFIRM_HEALTH_RECORD = "CONFIRM_HEALTH_RECORD";
    public static final String UPDATE_HEALTH_RECORD_METRICS = "UPDATE_HEALTH_RECORD_METRICS";
    public static final String DOWNLOAD_HEALTH_RECORD_PDF = "DOWNLOAD_HEALTH_RECORD_PDF";
    public static final String DELETE_HEALTH_RECORD = "DELETE_HEALTH_RECORD";

    public static final String INVITE_PROFILE_SHARE = "INVITE_PROFILE_SHARE";
    public static final String CANCEL_PROFILE_INVITATION = "CANCEL_PROFILE_INVITATION";
    public static final String ACCEPT_PROFILE_INVITATION = "ACCEPT_PROFILE_INVITATION";
    public static final String REJECT_PROFILE_INVITATION = "REJECT_PROFILE_INVITATION";
    public static final String REVOKE_PROFILE_SHARE = "REVOKE_PROFILE_SHARE";
    public static final String RESEND_PROFILE_INVITATION = "RESEND_PROFILE_INVITATION";

    public static final String INVITE_HEALTH_RECORD_SHARE = "INVITE_HEALTH_RECORD_SHARE";
    public static final String ACCEPT_HEALTH_RECORD_SHARE = "ACCEPT_HEALTH_RECORD_SHARE";
    public static final String REVOKE_HEALTH_RECORD_SHARE = "REVOKE_HEALTH_RECORD_SHARE";

    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String REGISTER = "REGISTER";
    public static final String VERIFY_EMAIL = "VERIFY_EMAIL";
    public static final String VERIFY_EMAIL_FAILED = "VERIFY_EMAIL_FAILED";
    public static final String REFRESH_TOKEN = "REFRESH_TOKEN";
    public static final String REFRESH_TOKEN_REUSE_FAILED = "REFRESH_TOKEN_REUSE_FAILED";
    public static final String FORGOT_PASSWORD = "FORGOT_PASSWORD";
    public static final String EMAIL_PROVIDER_FAILURE = "EMAIL_PROVIDER_FAILURE";
    public static final String RESET_PASSWORD = "RESET_PASSWORD";
    public static final String ADMIN_LOGIN = "ADMIN_LOGIN";
    public static final String ADMIN_TOTP_SETUP = "ADMIN_TOTP_SETUP";
    public static final String ADMIN_TOTP_VERIFY = "ADMIN_TOTP_VERIFY";

    public static final String UPDATE_USER = "UPDATE_USER";
    public static final String REQUEST_ACCOUNT_DELETION = "REQUEST_ACCOUNT_DELETION";
    public static final String CANCEL_ACCOUNT_DELETION = "CANCEL_ACCOUNT_DELETION";

    public static final String RECORD_CONSENT = "RECORD_CONSENT";
    public static final String REVOKE_CONSENT = "REVOKE_CONSENT";

    public static final String CREATE_PROFILE = "CREATE_PROFILE";
    public static final String UPDATE_PROFILE = "UPDATE_PROFILE";
}
