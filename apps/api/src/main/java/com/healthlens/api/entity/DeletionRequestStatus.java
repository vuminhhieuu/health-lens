package com.healthlens.api.entity;

/**
 * Status of a data deletion request.
 * PENDING: Request submitted, within 72-hour grace period
 * CANCELLED: User cancelled the deletion request
 * COMPLETED: 72 hours elapsed, data deleted
 */
public enum DeletionRequestStatus {
    PENDING,
    CANCELLED,
    COMPLETED
}
