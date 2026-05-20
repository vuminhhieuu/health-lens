package com.healthlens.api.audit;

public final class AuditResourceTypes {
    private AuditResourceTypes() {}

    /** Reference metric / ranges / aliases managed by admins */
    public static final String REFERENCE_DATA = "REFERENCE_DATA";

    /** End-user health records (view/delete and related mutations) */
    public static final String HEALTH_RECORD = "HEALTH_RECORD";

    /** Family profiles and sharing */
    public static final String PROFILE = "PROFILE";

    /** Login, registration, tokens */
    public static final String AUTH = "AUTH";

    /** User account settings and lifecycle */
    public static final String USER = "USER";

    /** Privacy / terms consent */
    public static final String CONSENT = "CONSENT";

    /** OCR background jobs */
    public static final String OCR_JOB = "OCR_JOB";

    /** AI chat provider calls */
    public static final String LLM_CALL = "LLM_CALL";

    /** Retrieval-augmented generation lookups */
    public static final String RAG_RETRIEVAL = "RAG_RETRIEVAL";
}
