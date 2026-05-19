package com.healthlens.api.repository;

import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.repository.projection.MonthlyUserGrowthProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByEmailIgnoreCase(String email);

    long countByRole(UserRole role);

    @Query("""
        SELECT COUNT(u) FROM User u
        WHERE u.role = :role
          AND u.accountStatus <> com.healthlens.api.entity.AccountStatus.DELETED
        """)
    long countRegisteredProductUsers(@Param("role") UserRole role);

    @Query(value = """
        SELECT CAST(DATE_TRUNC('month', u.created_at) AS DATE) AS month_start,
               COUNT(*) AS new_users
        FROM users u
        WHERE u.role = 'ROLE_USER'
          AND u.created_at >= :from
          AND u.created_at < :toExclusive
        GROUP BY 1
        ORDER BY 1
        """, nativeQuery = true)
    List<MonthlyUserGrowthProjection> findMonthlyUserGrowth(
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(UUID id);
}
