package com.healthlens.api.repository;

import com.healthlens.api.entity.NotificationInboxReadState;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationInboxReadStateRepository extends JpaRepository<NotificationInboxReadState, UUID> {

    Optional<NotificationInboxReadState> findByUserIdAndInboxItemId(UUID userId, String inboxItemId);

    @Query("""
            SELECT r.inboxItemId
            FROM NotificationInboxReadState r
            WHERE r.userId = :userId
              AND r.inboxItemId IN :inboxItemIds
            """)
    List<String> findReadInboxItemIds(
            @Param("userId") UUID userId, @Param("inboxItemIds") Collection<String> inboxItemIds);

    List<NotificationInboxReadState> findByUserIdAndItemTypeIsNotNullAndInboxItemIdNotIn(
            UUID userId, Collection<String> activeInboxItemIds);

    List<NotificationInboxReadState> findByUserIdAndItemTypeIsNotNullOrderByItemCreatedAtDesc(UUID userId);
}
