package com.healthlens.api.entity;

public enum OcrJobState {
    QUEUED,
    PROCESSING,
    SUCCEEDED,
    FAILED_RETRYABLE,
    FAILED_TERMINAL,
    DEAD_LETTERED
}
