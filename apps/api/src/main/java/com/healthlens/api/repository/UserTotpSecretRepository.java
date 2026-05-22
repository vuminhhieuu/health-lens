package com.healthlens.api.repository;

import com.healthlens.api.entity.UserTotpSecret;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserTotpSecretRepository extends JpaRepository<UserTotpSecret, UUID> {
    Optional<UserTotpSecret> findByUserId(UUID userId);
}
