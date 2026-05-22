package com.healthlens.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_totp_secrets")
public class UserTotpSecret {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "encrypted_secret", nullable = false)
    private String encryptedSecret;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "backup_codes_hash", columnDefinition = "TEXT")
    private String backupCodesHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
