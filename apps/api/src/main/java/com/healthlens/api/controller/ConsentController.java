package com.healthlens.api.controller;

import com.healthlens.api.constants.ConsentConstants;
import com.healthlens.api.dto.request.ConsentRequest;
import com.healthlens.api.dto.response.ConsentResponse;
import com.healthlens.api.dto.response.ConsentVersionResponse;
import com.healthlens.api.security.ClientIpResolver;
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

@RestController
@RequestMapping("/api/v1/users/me/consent")
public class ConsentController {

    /** Mirrors {@link ConsentConstants#ACTIVE_VERSION} for callers that reference the controller. */
    public static final String ACTIVE_CONSENT_VERSION = ConsentConstants.ACTIVE_VERSION;
    
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
        
        String ipAddress = ClientIpResolver.resolve(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        
        consentService.recordConsent(userId, request, ipAddress, userAgent);
        
        return ResponseEntity.ok().build();
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
