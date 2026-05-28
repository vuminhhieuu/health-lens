package com.healthlens.api.repository;

import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.entity.FollowUpReminder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

@Repository
public interface FollowUpReminderRepository extends JpaRepository<FollowUpReminder, UUID> {

    List<FollowUpReminder> findAllByProfileIdOrderByReminderDateAscCreatedAtAsc(UUID profileId);

    Optional<FollowUpReminder> findByIdAndProfileId(UUID id, UUID profileId);

    @EntityGraph(attributePaths = {"profile", "profile.user"})
    Optional<FollowUpReminder> findWithProfileAndUserById(UUID id);

    @EntityGraph(attributePaths = {"profile"})
    @Query("""
            SELECT reminder
            FROM FollowUpReminder reminder
            WHERE reminder.profile.user.id = :userId
              AND reminder.reminderDate >= :startDate
              AND reminder.reminderDate <= :horizon
            ORDER BY reminder.reminderDate ASC, reminder.createdAt ASC
            """)
    List<FollowUpReminder> findActiveRemindersForInbox(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("horizon") LocalDate horizon
    );

    @Query("""
            SELECT reminder.id
            FROM FollowUpReminder reminder
            WHERE reminder.emailSentAt IS NULL
              AND reminder.emailSkippedOptOutAt IS NULL
              AND reminder.reminderDate <= :today
              AND reminder.profile.user.accountStatus = :activeStatus
              AND (reminder.emailClaimedAt IS NULL OR reminder.emailClaimedAt < :claimCutoff)
            ORDER BY reminder.reminderDate ASC, reminder.createdAt ASC
            """)
    List<UUID> findDueReminderIdsForEmail(
            @Param("today") LocalDate today,
            @Param("claimCutoff") Instant claimCutoff,
            @Param("activeStatus") AccountStatus activeStatus,
            Pageable pageable
    );

    @Query("""
            SELECT reminder.id
            FROM FollowUpReminder reminder
            WHERE reminder.profile.user.id = :userId
              AND reminder.emailSentAt IS NULL
              AND reminder.emailSkippedOptOutAt IS NULL
              AND reminder.reminderDate <= :today
              AND reminder.profile.user.accountStatus = :activeStatus
              AND (reminder.emailClaimedAt IS NULL OR reminder.emailClaimedAt < :claimCutoff)
            ORDER BY reminder.reminderDate ASC, reminder.createdAt ASC
            """)
    List<UUID> findDueReminderIdsForUser(
            @Param("userId") UUID userId,
            @Param("today") LocalDate today,
            @Param("claimCutoff") Instant claimCutoff,
            @Param("activeStatus") AccountStatus activeStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FollowUpReminder reminder
            SET reminder.emailClaimedAt = :claimedAt
            WHERE reminder.id = :reminderId
              AND reminder.emailSentAt IS NULL
              AND reminder.emailSkippedOptOutAt IS NULL
              AND reminder.reminderDate <= :today
              AND (reminder.emailClaimedAt IS NULL OR reminder.emailClaimedAt < :claimCutoff)
              AND reminder.id IN (
                  SELECT activeReminder.id
                  FROM FollowUpReminder activeReminder
                  WHERE activeReminder.id = :reminderId
                    AND activeReminder.profile.user.accountStatus = :activeStatus
              )
            """)
    int claimDueReminderForEmail(
            @Param("reminderId") UUID reminderId,
            @Param("today") LocalDate today,
            @Param("claimedAt") Instant claimedAt,
            @Param("claimCutoff") Instant claimCutoff,
            @Param("activeStatus") AccountStatus activeStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FollowUpReminder reminder
            SET reminder.emailSentAt = :sentAt,
                reminder.emailClaimedAt = NULL
            WHERE reminder.id = :reminderId
              AND reminder.emailSentAt IS NULL
            """)
    int markEmailSent(
            @Param("reminderId") UUID reminderId,
            @Param("sentAt") Instant sentAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FollowUpReminder reminder
            SET reminder.emailClaimedAt = NULL
            WHERE reminder.id = :reminderId
              AND reminder.emailSentAt IS NULL
            """)
    int releaseEmailClaim(
            @Param("reminderId") UUID reminderId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FollowUpReminder reminder
            SET reminder.emailSkippedOptOutAt = :skippedAt,
                reminder.emailClaimedAt = NULL
            WHERE reminder.id = :reminderId
              AND reminder.emailSentAt IS NULL
              AND reminder.emailSkippedOptOutAt IS NULL
            """)
    int markEmailSkippedOptOut(
            @Param("reminderId") UUID reminderId,
            @Param("skippedAt") Instant skippedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FollowUpReminder reminder
            SET reminder.emailSkippedOptOutAt = NULL,
                reminder.emailClaimedAt = NULL
            WHERE reminder.emailSentAt IS NULL
              AND reminder.emailSkippedOptOutAt IS NOT NULL
              AND reminder.reminderDate <= :today
              AND reminder.profile.user.id = :userId
            """)
    int clearEmailSkippedOptOutForUser(
            @Param("userId") UUID userId,
            @Param("today") LocalDate today
    );

    @Modifying
    @Query("""
            DELETE FROM FollowUpReminder reminder
            WHERE reminder.profile.user.id = :userId
            """)
    int deleteAllByUserId(@Param("userId") UUID userId);
}
