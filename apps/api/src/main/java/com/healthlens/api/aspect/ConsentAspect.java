package com.healthlens.api.aspect;

import com.healthlens.api.annotation.RequiresConsent;
import com.healthlens.api.constants.ConsentConstants;
import com.healthlens.api.exception.ConsentRequiredException;
import com.healthlens.api.service.ConsentService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class ConsentAspect {

    private static final Logger logger = LoggerFactory.getLogger(ConsentAspect.class);

    private final ConsentService consentService;

    public ConsentAspect(ConsentService consentService) {
        this.consentService = consentService;
    }

    @Before("@annotation(com.healthlens.api.annotation.RequiresConsent) || @within(com.healthlens.api.annotation.RequiresConsent)")
    public void checkConsent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            logger.debug("No valid authentication found for consent check");
            return;
        }

        try {
            String principalStr = (String) auth.getPrincipal();
            UUID userId = UUID.fromString(principalStr);
            
            logger.debug("Checking consent for user: {}", userId);
            boolean hasConsent = consentService.hasConsent(userId, ConsentConstants.ACTIVE_VERSION);
            
            if (!hasConsent) {
                logger.warn("User {} attempted to access protected resource without valid consent", userId);
                throw new ConsentRequiredException("User consent is required for this action");
            }
            logger.debug("User {} has valid consent", userId);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid UUID format in authentication principal", e);
            throw new ConsentRequiredException("Invalid authentication context");
        }
    }
}
