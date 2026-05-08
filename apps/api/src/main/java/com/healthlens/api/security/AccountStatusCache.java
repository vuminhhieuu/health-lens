package com.healthlens.api.security;

import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.entity.User;
import com.healthlens.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Caches {@link AccountStatus} per user in Redis to avoid a DB read on every authenticated request.
 * Status changes are written through from {@link com.healthlens.api.service.DataDeletionService} so
 * revocations stay correct without waiting for TTL expiry.
 */
@Component
public class AccountStatusCache {

    private static final Logger log = LoggerFactory.getLogger(AccountStatusCache.class);
    static final String KEY_PREFIX = "account:status:";

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final Duration ttl;

    public AccountStatusCache(
            StringRedisTemplate redisTemplate,
            UserRepository userRepository,
            @Value("${app.security.account-status-cache-ttl:60s}") Duration accountStatusCacheTtl) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.ttl = accountStatusCacheTtl;
    }

    /**
     * Resolves account status for JWT authentication: cache hit, else load user from DB and populate cache.
     *
     * @return empty if the user does not exist
     */
    public Optional<AccountStatus> getStatus(UUID userId) {
        String key = KEY_PREFIX + userId;
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return Optional.of(AccountStatus.valueOf(cached));
            }
        } catch (Exception e) {
            log.warn("Redis account-status cache read failed, falling back to DB: userId={}", userId, e);
        }

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return Optional.empty();
        }
        AccountStatus status = user.get().getAccountStatus();
        try {
            redisTemplate.opsForValue().set(key, status.name(), ttl);
        } catch (Exception e) {
            log.warn("Redis account-status cache write failed: userId={}", userId, e);
        }
        return Optional.of(status);
    }

    /**
     * Call after persisting a new {@link AccountStatus} so subsequent requests see it immediately.
     */
    public void put(UUID userId, AccountStatus status) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + userId, status.name(), ttl);
        } catch (Exception e) {
            log.warn("Redis account-status cache put failed: userId={} status={}", userId, status, e);
        }
    }
}
