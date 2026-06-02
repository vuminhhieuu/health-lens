package com.healthlens.api.repository;

import com.healthlens.api.entity.HealthRecord;
import com.healthlens.api.support.PostgresTestContainerBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class HealthRecordSoftDeleteIT extends PostgresTestContainerBase {

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void delete_marksDeletedAndRepositoryQueriesDoNotReturnRecord() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO users
                    (id, email, full_name, date_of_birth, password_hash, email_verified, role, account_status)
                VALUES (?, ?, 'Soft Delete User', DATE '1990-01-01', '[hash]', true, 'ROLE_USER', 'ACTIVE')
                """, userId, "soft-delete-%s@example.com".formatted(userId));
        jdbcTemplate.update("""
                INSERT INTO profiles (id, user_id, display_name, is_default)
                VALUES (?, ?, 'Soft Delete Profile', true)
                """, profileId, userId);

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
        record.setProfileId(profileId);
        record.setFileKey("health-records/%s/%s/original.pdf".formatted(userId, recordId));
        healthRecordRepository.saveAndFlush(record);

        healthRecordRepository.delete(record);
        healthRecordRepository.flush();

        assertThat(healthRecordRepository.findAllByUserId(userId)).isEmpty();
        assertThat(healthRecordRepository.findAllByProfileIdAndUserId(profileId, userId, org.springframework.data.domain.Pageable.unpaged()))
                .isEmpty();
        assertThat(healthRecordRepository.findFileKeysByUserId(userId)).isEmpty();
        assertThat(healthRecordRepository.findAllFileKeysByUserIdIncludingDeleted(userId))
                .containsExactly("health-records/%s/%s/original.pdf".formatted(userId, recordId));

        java.sql.Timestamp deletedAt = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM health_records WHERE id = ?",
                java.sql.Timestamp.class,
                recordId
        );
        assertThat(deletedAt).isNotNull();

        assertThat(healthRecordRepository.deleteAllByUserId(userId)).isEqualTo(1);
        Integer remainingRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM health_records WHERE id = ?",
                Integer.class,
                recordId
        );
        assertThat(remainingRows).isZero();
    }
}
