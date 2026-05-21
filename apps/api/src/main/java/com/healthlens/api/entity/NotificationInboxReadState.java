package com.healthlens.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "notification_inbox_read_state")
public class NotificationInboxReadState {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "inbox_item_id", nullable = false, length = 120)
    private String inboxItemId;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    @Column(name = "item_type", length = 40)
    private String itemType;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "item_created_at")
    private Instant itemCreatedAt;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (readAt == null) {
            readAt = Instant.now();
        }
    }
}
