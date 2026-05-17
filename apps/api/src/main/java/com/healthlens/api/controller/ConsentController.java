package com.healthlens.api.controller;

import com.healthlens.api.constants.ConsentConstants;
import com.healthlens.api.dto.request.ConsentRequest;
import com.healthlens.api.dto.response.ConsentResponse;
import com.healthlens.api.dto.response.ConsentVersionResponse;
import com.healthlens.api.service.ConsentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/users/me/consent")
public class ConsentController {

    /** Mirrors {@link ConsentConstants#ACTIVE_VERSION} for callers that reference the controller. */
    public static final String ACTIVE_CONSENT_VERSION = ConsentConstants.ACTIVE_VERSION;
    
    // IP address validation pattern (IPv4 and IPv6)
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}" +
        "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
    );
    
    private static final Pattern IPV6_PATTERN = Pattern.compile(
        "^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|" +
        "([0-9a-fA-F]{1,4}:){1,7}:|" +
        "([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4})$"
    );

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping
    public ResponseEntity<ConsentResponse> getConsentStatus(Authentication authentication) {
        validatePrincipal(authentication);
        UUID userId = UUID.fromString((String) authentication.getPrincipal());
        return ResponseEntity.ok(consentService.getConsentStatus(userId, ACTIVE_CONSENT_VERSION));
    }

    @GetMapping("/active-version")
    public ResponseEntity<ConsentVersionResponse> getActiveConsentVersion() {
        return ResponseEntity.ok(new ConsentVersionResponse(ACTIVE_CONSENT_VERSION));
    }

    @PostMapping
    public ResponseEntity<Void> recordConsent(@Valid @RequestBody ConsentRequest request,
                                              HttpServletRequest servletRequest,
                                              Authentication authentication) {
        validatePrincipal(authentication);
        UUID userId = UUID.fromString((String) authentication.getPrincipal());
        
        String ipAddress = extractClientIpAddress(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        
        consentService.recordConsent(userId, request, ipAddress, userAgent);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Extracts the client IP address from the request, considering X-Forwarded-For header.
     * Takes the first IP from the chain and validates format.
     */
    private String extractClientIpAddress(HttpServletRequest servletRequest) {
        String xForwardedFor = servletRequest.getHeader("X-Forwarded-For");
        
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs separated by comma
            String[] ips = xForwardedFor.split(",");
            String firstIp = ips[0].trim();
            
            // Validate IP format
            if (isValidIpAddress(firstIp)) {
                return firstIp;
            }
        }
        
        // Fallback to direct remote address
        String remoteAddr = servletRequest.getRemoteAddr();
        return isValidIpAddress(remoteAddr) ? remoteAddr : "unknown";
    }

    /**
     * Validates if the given string is a valid IPv4 or IPv6 address.
     */
    private boolean isValidIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equals(ipAddress)) {
            return false;
        }
        return IPV4_PATTERN.matcher(ipAddress).matches() || 
               IPV6_PATTERN.matcher(ipAddress).matches();
    }

    /**
     * Validates that the authentication principal is present and valid.
     */
    private void validatePrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cần đăng nhập để thực hiện thao tác này");
        }

        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal.toString())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ");
        }
    }
}
