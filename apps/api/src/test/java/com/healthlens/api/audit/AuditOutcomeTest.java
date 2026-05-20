package com.healthlens.api.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditOutcomeTest {

    @Test
    void fromAction_marksExplicitFailureActionsAsFailure() {
        assertThat(AuditOutcome.fromAction(AuditActions.LOGIN_FAILED)).isEqualTo(AuditOutcome.FAILURE);
        assertThat(AuditOutcome.fromAction(AuditActions.OCR_JOB_FAILED_RETRYABLE)).isEqualTo(AuditOutcome.FAILURE);
        assertThat(AuditOutcome.fromAction(AuditActions.OCR_JOB_FAILED_TERMINAL)).isEqualTo(AuditOutcome.FAILURE);
        assertThat(AuditOutcome.fromAction(AuditActions.OCR_JOB_DEAD_LETTERED)).isEqualTo(AuditOutcome.FAILURE);
        assertThat(AuditOutcome.fromAction(AuditActions.LLM_CALL_FAILED)).isEqualTo(AuditOutcome.FAILURE);
    }

    @Test
    void fromAction_keepsSuccessActionsAsSuccess() {
        assertThat(AuditOutcome.fromAction(AuditActions.OCR_JOB_SUCCEEDED)).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(AuditOutcome.fromAction(AuditActions.RAG_RETRIEVAL)).isEqualTo(AuditOutcome.SUCCESS);
    }
}
