package com.healthlens.api.activity;

public final class UserActivityEventType {

    public static final String AUTHENTICATED_API_CALL = "AUTHENTICATED_API_CALL";
    public static final String USER_REGISTERED = "USER_REGISTERED";
    public static final String UPLOAD_STARTED = "UPLOAD_STARTED";
    public static final String UPLOAD_CONFIRMED = "UPLOAD_CONFIRMED";
    public static final String OCR_COMPLETED = "OCR_COMPLETED";
    public static final String OCR_FAILED = "OCR_FAILED";

    private UserActivityEventType() {
    }
}
