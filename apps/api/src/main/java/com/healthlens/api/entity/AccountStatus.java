package com.healthlens.api.entity;

/**
 * Account status enum for user lifecycle management.
 * ACTIVE: Normal account, can log in
 * PENDING_DELETION: Deletion request submitted, 72-hour grace period active
 * DELETED: Data deletion completed, account archived
 */
public enum AccountStatus {
    ACTIVE,
    PENDING_DELETION,
    DELETED
}
