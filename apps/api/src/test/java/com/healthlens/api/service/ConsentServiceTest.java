package com.healthlens.api.service;

import com.healthlens.api.dto.request.ConsentRequest;
import com.healthlens.api.dto.response.ConsentResponse;
import com.healthlens.api.entity.ConsentLog;
import com.healthlens.api.entity.User;
import com.healthlens.api.repository.ConsentLogRepository;
import com.healthlens.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock
    private ConsentLogRepository consentLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.healthlens.api.audit.AuditEventRecorder auditEventRecorder;

    private ConsentService consentService;

    private final UUID userId = UUID.randomUUID();
    private final String activeVersion = "1.0";
    private User mockUser;

    @BeforeEach
    void setUp() {
        consentService = new ConsentService(consentLogRepository, userRepository, auditEventRecorder);
        mockUser = new User();
        mockUser.setId(userId);
    }

    @Test
    void getConsentStatus_WhenNoConsent_ReturnsFalse() {
        when(consentLogRepository.findFirstByUser_IdOrderByConsentedAtDesc(userId))
                .thenReturn(Optional.empty());

        ConsentResponse response = consentService.getConsentStatus(userId, activeVersion);

        assertThat(response.isConsentGiven()).isFalse();
        assertThat(response.getConsentVersion()).isNull();
    }

    @Test
    void getConsentStatus_WhenConsentRevoked_ReturnsFalse() {
        ConsentLog log = new ConsentLog();
        log.setConsentVersion(activeVersion);
        log.setRevokedAt(Instant.now());

        when(consentLogRepository.findFirstByUser_IdOrderByConsentedAtDesc(userId))
                .thenReturn(Optional.of(log));

        ConsentResponse response = consentService.getConsentStatus(userId, activeVersion);

        assertThat(response.isConsentGiven()).isFalse();
        assertThat(response.getConsentVersion()).isNull();
    }

    @Test
    void getConsentStatus_WhenConsentOutdated_ReturnsFalse() {
        ConsentLog log = new ConsentLog();
        log.setConsentVersion("0.9");

        when(consentLogRepository.findFirstByUser_IdOrderByConsentedAtDesc(userId))
                .thenReturn(Optional.of(log));

        ConsentResponse response = consentService.getConsentStatus(userId, activeVersion);

        assertThat(response.isConsentGiven()).isFalse();
        assertThat(response.getConsentVersion()).isEqualTo("0.9");
    }

    @Test
    void getConsentStatus_WhenConsentUpToDate_ReturnsTrue() {
        ConsentLog log = new ConsentLog();
        log.setConsentVersion(activeVersion);

        when(consentLogRepository.findFirstByUser_IdOrderByConsentedAtDesc(userId))
                .thenReturn(Optional.of(log));

        ConsentResponse response = consentService.getConsentStatus(userId, activeVersion);

        assertThat(response.isConsentGiven()).isTrue();
        assertThat(response.getConsentVersion()).isEqualTo(activeVersion);
    }

    @Test
    void recordConsent_WhenAccepting_SavesConsentLog() {
        ConsentRequest request = new ConsentRequest();
        request.setVersion(activeVersion);
        request.setAccepted(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        consentService.recordConsent(userId, request, "127.0.0.1", "TestBot/1.0");

        ArgumentCaptor<ConsentLog> logCaptor = ArgumentCaptor.forClass(ConsentLog.class);
        verify(consentLogRepository).save(logCaptor.capture());

        ConsentLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getUser()).isEqualTo(mockUser);
        assertThat(savedLog.getConsentVersion()).isEqualTo(activeVersion);
        assertThat(savedLog.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(savedLog.getUserAgent()).isEqualTo("TestBot/1.0");
    }

    @Test
    void recordConsent_WhenRejecting_RevokesCurrentConsent() {
        ConsentRequest request = new ConsentRequest();
        request.setVersion(activeVersion);
        request.setAccepted(false);

        ConsentLog currentLog = new ConsentLog();
        currentLog.setConsentVersion(activeVersion);

        when(consentLogRepository.findFirstByUser_IdOrderByConsentedAtDesc(userId))
                .thenReturn(Optional.of(currentLog));

        consentService.recordConsent(userId, request, "127.0.0.1", "TestBot/1.0");

        ArgumentCaptor<ConsentLog> logCaptor = ArgumentCaptor.forClass(ConsentLog.class);
        verify(consentLogRepository).save(logCaptor.capture());

        ConsentLog updatedLog = logCaptor.getValue();
        assertThat(updatedLog.getRevokedAt()).isNotNull();
    }
}
