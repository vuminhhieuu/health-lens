package com.healthlens.api.service;

import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.request.ConsentRequest;
import com.healthlens.api.dto.response.ConsentResponse;
import com.healthlens.api.entity.ConsentLog;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.UserNotFoundException;
import com.healthlens.api.repository.ConsentLogRepository;
import com.healthlens.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConsentService {

    private final ConsentLogRepository consentLogRepository;
    private final UserRepository userRepository;
    private final AuditEventRecorder auditEventRecorder;

    public ConsentService(
            ConsentLogRepository consentLogRepository,
            UserRepository userRepository,
            AuditEventRecorder auditEventRecorder
    ) {
        this.consentLogRepository = consentLogRepository;
        this.userRepository = userRepository;
        this.auditEventRecorder = auditEventRecorder;
    }

    @Transactional(readOnly = true)
    public ConsentResponse getConsentStatus(UUID userId, String activeConsentVersion) {
        Optional<ConsentLog> latestConsent = consentLogRepository.findFirstByUser_IdOrderByConsentedAtDesc(userId);

        if (latestConsent.isPresent() && latestConsent.get().getRevokedAt() == null) {
            String userConsentVersion = latestConsent.get().getConsentVersion();
            boolean isUpToDate = activeConsentVersion.equals(userConsentVersion);
            return ConsentResponse.builder()
                    .consentGiven(isUpToDate)
                    .consentVersion(userConsentVersion)
                    .consentedAt(latestConsent.get().getConsentedAt())
                    .build();
        }

        return ConsentResponse.builder()
                .consentGiven(false)
                .consentVersion(null)
                .build();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void recordConsent(UUID userId, ConsentRequest request, String ipAddress, String userAgent) {
        if (!Boolean.TRUE.equals(request.getAccepted())) {
            Optional<ConsentLog> latestConsent = consentLogRepository.findFirstByUser_IdOrderByConsentedAtDesc(userId);
            latestConsent.ifPresent(consent -> {
                if (consent.getRevokedAt() == null) {
                    consent.setRevokedAt(Instant.now());
                    consentLogRepository.save(consent);
                    auditEventRecorder.recordEvent(
                            userId,
                            AuditActions.REVOKE_CONSENT,
                            AuditResourceTypes.CONSENT,
                            consent.getId(),
                            Map.of("version", consent.getConsentVersion())
                    );
                }
            });
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với mã: " + userId));

        ConsentLog log = new ConsentLog();
        log.setUser(user);
        log.setConsentVersion(request.getVersion());
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);

        consentLogRepository.save(log);

        auditEventRecorder.recordEvent(
                userId,
                AuditActions.RECORD_CONSENT,
                AuditResourceTypes.CONSENT,
                log.getId(),
                Map.of("version", request.getVersion())
        );
    }
    
    @Transactional(readOnly = true)
    public boolean hasConsent(UUID userId, String activeConsentVersion) {
        return getConsentStatus(userId, activeConsentVersion).isConsentGiven();
    }
}
