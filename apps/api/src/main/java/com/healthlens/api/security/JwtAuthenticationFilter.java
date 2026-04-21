package com.healthlens.api.security;

import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.entity.User;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:token:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final boolean securityFailClosed;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            StringRedisTemplate redisTemplate,
            UserRepository userRepository,
            @Value("${app.security.blacklist-fail-closed:false}") boolean securityFailClosed) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.securityFailClosed = securityFailClosed;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        log.info("Incoming request: {}", path);

        if (path.contains("/deletion-requests/cancel")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        if (!jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if token has been blacklisted (logout)
        String jti = jwtUtil.extractJti(token);
        if (isTokenBlacklisted(jti)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract user info from token
        String userId = jwtUtil.extractSubject(token);
        String role = jwtUtil.extractClaims(token).get("role", String.class);

        // AC #3: Check if account is pending deletion or deleted
        try {
            User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
            if (user == null) {
                log.warn("User not found: {}", userId);
                filterChain.doFilter(request, response);
                return;
            }

            if (user.getAccountStatus() == AccountStatus.PENDING_DELETION) {
                log.warn("User pending deletion: {}", userId);
            }
        } catch (Exception e) {
            log.error("Error checking account status for user: {}", userId, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try {
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Lỗi hệ thống\"}");
                response.getWriter().flush();
            } catch (IOException ioe) {
                log.error("Failed to write error response: {}", ioe.getMessage());
            }
            return;
        }

        // Set SecurityContext with authenticated user
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private boolean isTokenBlacklisted(String jti) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
        } catch (Exception e) {
            log.error("Redis unavailable when checking token blacklist. Fail-closed: {}. Token JTI: {}", securityFailClosed, jti, e);
            return securityFailClosed;
        }
    }
}
