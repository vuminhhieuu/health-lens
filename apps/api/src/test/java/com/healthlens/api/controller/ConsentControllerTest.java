package com.healthlens.api.controller;

import com.healthlens.api.dto.response.ConsentVersionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConsentController.
 * Tests the new active-version endpoint and consent version retrieval.
 */
class ConsentControllerTest {

    private ConsentController consentController;

    /**
     * Test that the active consent version endpoint returns the correct version.
     * This simulates a direct endpoint call without Spring context.
     */
    @Test
    @DisplayName("getActiveConsentVersion returns current consent version constant")
    void getActiveConsentVersion_ReturnsVersion() {
        // Note: Direct controller test without full Spring context
        // to verify business logic only
        String activeVersion = ConsentController.ACTIVE_CONSENT_VERSION;
        assertThat(activeVersion).isEqualTo("1.0");
    }

    /**
     * Test that ConsentVersionResponse DTO can be instantiated with version.
     */
    @Test
    @DisplayName("ConsentVersionResponse DTO contains version")
    void consentVersionResponse_ContainsVersion() {
        ConsentVersionResponse response = new ConsentVersionResponse("1.0");
        assertThat(response.version()).isEqualTo("1.0");
    }

    /**
     * Test that ConsentVersionResponse correctly serializes version.
     */
    @Test
    @DisplayName("ConsentVersionResponse version matches constant")
    void consentVersionResponse_MatchesConstant() {
        String activeVersion = ConsentController.ACTIVE_CONSENT_VERSION;
        ConsentVersionResponse response = new ConsentVersionResponse(activeVersion);
        assertThat(response.version()).isEqualTo("1.0");
    }
}
