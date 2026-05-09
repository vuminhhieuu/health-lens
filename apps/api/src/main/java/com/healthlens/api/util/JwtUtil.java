package com.healthlens.api.util;

import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long accessTtl;
    private final long refreshTtl;
    private final long adminAccessTtl;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-ttl}") long accessTtl,
            @Value("${jwt.refresh-ttl}") long refreshTtl,
            @Value("${jwt.admin-access-ttl:900000}") long adminAccessTtl
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
        this.adminAccessTtl = adminAccessTtl;
    }

    /**
     * Generate an access token (Bearer) for the given user.
     * Claims: sub=userId, email, role, jti (unique ID), iat, exp (15m).
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTtl);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generate a random refresh token string (not a JWT).
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Validate an access token: parse, verify signature, check expiry.
     * Returns true if valid, false otherwise.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extract all claims from a valid access token.
     * Caller should validate the token first.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract the JTI (JWT ID) claim used for blacklisting at logout.
     */
    public String extractJti(String token) {
        return extractClaims(token).getId();
    }

    /**
     * Extract the subject (user ID) from the token.
     */
    public String extractSubject(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Get the remaining milliseconds before the token expires.
     * Used to set Redis TTL for blacklisted tokens.
     */
    public long getRemainingExpiry(String token) {
        try {
            Claims claims = extractClaims(token);
            long expMillis = claims.getExpiration().getTime();
            long remaining = expMillis - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (ExpiredJwtException e) {
            return 0;
        }
    }

    public long getAccessTtl() {
        return accessTtl;
    }

    public long getRefreshTtl() {
        return refreshTtl;
    }

    /**
     * Generate an admin access token with totpVerified claim.
     * Admin tokens expire in 15 minutes (admin session TTL).
     */
    public String generateAdminAccessToken(User user, boolean totpVerified) {
        if (user.getRole() != UserRole.ROLE_ADMIN) {
            throw new IllegalArgumentException("Cannot generate admin token for non-admin user");
        }
        Date now = new Date();
        Date expiry = new Date(now.getTime() + adminAccessTtl);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("totpVerified", totpVerified)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public long getAdminAccessTtl() {
        return adminAccessTtl;
    }
}
