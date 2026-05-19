package com.healthlens.api.security;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.constants.SecurityConstants;
import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:token:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final AccountStatusCache accountStatusCache;
    private final boolean securityFailClosed;
    private final UrlPathHelper urlPathHelper = new UrlPathHelper();

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            StringRedisTemplate redisTemplate,
            AccountStatusCache accountStatusCache,
            @Value("${app.security.blacklist-fail-closed:false}") boolean securityFailClosed) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.accountStatusCache = accountStatusCache;
        this.securityFailClosed = securityFailClosed;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = resolveAccessToken(request);
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

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

        var claims = jwtUtil.extractClaims(token);

        // Set SecurityContext with authenticated user
        String userId = jwtUtil.extractSubject(token);
        String role = claims.get("role", String.class);
        String email = claims.get("email", String.class);
        Boolean totpVerified = claims.get("totpVerified", Boolean.class);

        // AC #3: Block authenticated requests for accounts in PENDING_DELETION or
        // DELETED state.
        // The cancel endpoint is permitAll'd via SecurityConfig and never reaches here
        // with a token.
        try {
            Optional<AccountStatus> statusOpt = accountStatusCache.getStatus(UUID.fromString(userId));
            if (statusOpt.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            AccountStatus status = statusOpt.get();
            if (status == AccountStatus.PENDING_DELETION || status == AccountStatus.DELETED) {
                log.warn("Blocking request for {} account: userId={}, path={}",
                        status, userId, urlPathHelper.getRequestUri(request));
                writeAccountPendingDeletionResponse(response, status);
                return;
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid userId in JWT subject: {}", userId);
            filterChain.doFilter(request, response);
            return;
        } catch (Exception e) {
            log.error("Error checking account status for user: {}", userId, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"detail\":\"Lỗi hệ thống khi xác thực phiên đăng nhập\"}");
            return;
        }

        // For admin routes (non-auth), require totpVerified=true
        String requestUri = request.getRequestURI();
        if (requestUri.startsWith(ApiRoutes.ADMIN_BASE + "/") && !requestUri.startsWith(ApiRoutes.ADMIN_AUTH_BASE + "/")) {
            if (!Boolean.TRUE.equals(totpVerified)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Vui lòng xác thực TOTP trước khi truy cập khu vực quản trị");
                return;
            }
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority(role)));
        Map<String, Object> authenticationDetails = new HashMap<>();
        authenticationDetails.put("totpVerified", Boolean.TRUE.equals(totpVerified));
        if (email != null && !email.isBlank()) {
            authenticationDetails.put("email", email);
        }
        authentication.setDetails(authenticationDetails);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Public deletion-cancellation endpoint (token-based, no JWT). Avoid touching
        // SecurityContext entirely.
        String path = urlPathHelper.getRequestUri(request);
        return path != null && path.startsWith(ApiRoutes.USERS_DELETION_BASE);
    }

    private void writeAccountPendingDeletionResponse(HttpServletResponse response, AccountStatus status)
            throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String detail = status == AccountStatus.DELETED
                ? "Tài khoản đã bị xóa"
                : "Tài khoản đang chờ xóa";
        String body = "{\"type\":\"https://healthlens.vn/errors/account-pending-deletion\","
                + "\"title\":\"Tài khoản đang chờ xóa\","
                + "\"status\":403,"
                + "\"detail\":\"" + detail + "\","
                + "\"errorCode\":\"ACCOUNT_PENDING_DELETION\"}";
        response.getWriter().write(body);
    }

    private boolean isTokenBlacklisted(String jti) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
        } catch (Exception e) {
            log.error("Redis unavailable when checking token blacklist. Fail-closed: {}. Token JTI: {}",
                    securityFailClosed, jti, e);
            return securityFailClosed;
        }
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SecurityConstants.ADMIN_ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
