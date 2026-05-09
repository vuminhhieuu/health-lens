package com.healthlens.api.repository;

import com.healthlens.api.entity.AdminTotpSecret;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminTotpSecretRepository extends JpaRepository<AdminTotpSecret, UUID> {
    Optional<AdminTotpSecret> findByUserId(UUID userId);
}
