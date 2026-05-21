package com.healthlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthlens.api.dto.response.IncomingHealthRecordInvitationResponse;
import com.healthlens.api.dto.response.IncomingProfileInvitationResponse;
import com.healthlens.api.dto.response.NotificationInboxItemType;
import com.healthlens.api.entity.NotificationInboxReadState;
import com.healthlens.api.repository.NotificationInboxReadStateRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationInboxServiceTest {

    @Mock
    private ProfileShareService profileShareService;

    @Mock
    private HealthRecordShareService healthRecordShareService;

    @Mock
    private NotificationInboxReadStateRepository readStateRepository;

    @InjectMocks
    private NotificationInboxService notificationInboxService;

    @Test
    @DisplayName("listInbox merges invitations, sorts by createdAt desc, uses typed ids")
    void listInbox_mergesSortsAndUsesTypedIds() {
        UUID userId = UUID.randomUUID();
        Instant older = Instant.parse("2026-01-01T10:00:00Z");
        Instant newer = Instant.parse("2026-02-01T10:00:00Z");

        UUID profileInviteId = UUID.randomUUID();
        UUID recordInviteId = UUID.randomUUID();

        when(profileShareService.listIncomingInvitations(userId))
                .thenReturn(List.of(new IncomingProfileInvitationResponse(
                        profileInviteId,
                        UUID.randomUUID(),
                        "Hồ sơ An",
                        "Lan",
                        older.plusSeconds(3600),
                        older,
                        "view",
                        "/invitations/accept?token=abc"
                )));
        when(readStateRepository.findReadInboxItemIds(eq(userId), any())).thenReturn(List.of());
        when(healthRecordShareService.listIncomingInvitations(userId))
                .thenReturn(List.of(new IncomingHealthRecordInvitationResponse(
                        recordInviteId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Minh",
                        newer.plusSeconds(3600),
                        newer,
                        "/health-record-invitations/accept?token=xyz"
                )));

        var items = notificationInboxService.listInbox(userId);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).type()).isEqualTo(NotificationInboxItemType.HEALTH_RECORD_INVITATION);
        assertThat(items.get(0).createdAt()).isEqualTo(newer);
        assertThat(items.get(0).read()).isFalse();
        assertThat(items.get(0).id()).isEqualTo("HEALTH_RECORD_INVITATION:" + recordInviteId);
        assertThat(items.get(1).type()).isEqualTo(NotificationInboxItemType.PROFILE_INVITATION);
        assertThat(items.get(1).id()).isEqualTo("PROFILE_INVITATION:" + profileInviteId);
        assertThat(items.get(1).actionUrl()).contains("/invitations/accept");
    }

    @Test
    @DisplayName("listInbox caps merged results at 50 items")
    void listInbox_capsAt50() {
        UUID userId = UUID.randomUUID();
        Instant base = Instant.parse("2026-01-01T00:00:00Z");

        List<IncomingProfileInvitationResponse> profileInvites = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            profileInvites.add(new IncomingProfileInvitationResponse(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Profile " + i,
                    "Owner",
                    base.plusSeconds(3600 + i),
                    base.plusSeconds(i),
                    "view",
                    "/invitations/accept?token=p" + i
            ));
        }

        List<IncomingHealthRecordInvitationResponse> recordInvites = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            recordInvites.add(new IncomingHealthRecordInvitationResponse(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Owner",
                    base.plusSeconds(7200 + i),
                    base.plusSeconds(30 + i),
                    "/health-record-invitations/accept?token=r" + i
            ));
        }

        when(profileShareService.listIncomingInvitations(userId)).thenReturn(profileInvites);
        when(healthRecordShareService.listIncomingInvitations(userId)).thenReturn(recordInvites);
        when(readStateRepository.findReadInboxItemIds(eq(userId), any())).thenReturn(List.of());

        var items = notificationInboxService.listInbox(userId);

        assertThat(items).hasSize(50);
        assertThat(items.get(0).type()).isEqualTo(NotificationInboxItemType.HEALTH_RECORD_INVITATION);
        assertThat(items.get(0).createdAt()).isEqualTo(base.plusSeconds(59));
    }

    @Test
    @DisplayName("listInbox merges persisted read state for inbox item ids")
    void listInbox_appliesReadState() {
        UUID userId = UUID.randomUUID();
        UUID profileInviteId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        String itemId = "PROFILE_INVITATION:" + profileInviteId;

        when(profileShareService.listIncomingInvitations(userId))
                .thenReturn(List.of(new IncomingProfileInvitationResponse(
                        profileInviteId,
                        UUID.randomUUID(),
                        "Hồ sơ An",
                        "Lan",
                        createdAt.plusSeconds(3600),
                        createdAt,
                        "view",
                        "/invitations/accept?token=abc"
                )));
        when(healthRecordShareService.listIncomingInvitations(userId)).thenReturn(List.of());
        when(readStateRepository.findReadInboxItemIds(eq(userId), eq(List.of(itemId))))
                .thenReturn(List.of(itemId));

        var items = notificationInboxService.listInbox(userId);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).read()).isTrue();
    }

    @Test
    @DisplayName("markAsRead persists read state for inbox item belonging to user")
    void markAsRead_persistsState() {
        UUID userId = UUID.randomUUID();
        UUID profileInviteId = UUID.randomUUID();
        String itemId = "PROFILE_INVITATION:" + profileInviteId;

        when(profileShareService.listIncomingInvitations(userId))
                .thenReturn(List.of(new IncomingProfileInvitationResponse(
                        profileInviteId,
                        UUID.randomUUID(),
                        "Hồ sơ An",
                        "Lan",
                        Instant.parse("2026-01-01T11:00:00Z"),
                        Instant.parse("2026-01-01T10:00:00Z"),
                        "view",
                        "/invitations/accept?token=abc"
                )));
        when(healthRecordShareService.listIncomingInvitations(userId)).thenReturn(List.of());
        when(readStateRepository.findByUserIdAndInboxItemId(userId, itemId)).thenReturn(java.util.Optional.empty());

        notificationInboxService.markAsRead(userId, itemId);

        ArgumentCaptor<NotificationInboxReadState> captor = ArgumentCaptor.forClass(NotificationInboxReadState.class);
        verify(readStateRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getInboxItemId()).isEqualTo(itemId);
        assertThat(captor.getValue().getItemType()).isEqualTo(NotificationInboxItemType.PROFILE_INVITATION.name());
        assertThat(captor.getValue().getTitle()).isEqualTo("Lời mời xem hồ sơ");
    }

    @Test
    @DisplayName("listInbox keeps archived read items after pending invitation is gone")
    void listInbox_includesArchivedReadSnapshot() {
        UUID userId = UUID.randomUUID();
        String archivedId = "PROFILE_INVITATION:" + UUID.randomUUID();

        when(profileShareService.listIncomingInvitations(userId)).thenReturn(List.of());
        when(healthRecordShareService.listIncomingInvitations(userId)).thenReturn(List.of());

        NotificationInboxReadState archived = new NotificationInboxReadState();
        archived.setUserId(userId);
        archived.setInboxItemId(archivedId);
        archived.setReadAt(Instant.parse("2026-02-01T12:00:00Z"));
        archived.setItemType(NotificationInboxItemType.PROFILE_INVITATION.name());
        archived.setTitle("Lời mời xem hồ sơ");
        archived.setBody("Lan mời bạn xem hồ sơ \"Hồ sơ An\".");
        archived.setItemCreatedAt(Instant.parse("2026-02-01T10:00:00Z"));
        archived.setActionUrl("/invitations/accept?token=abc");

        when(readStateRepository.findByUserIdAndItemTypeIsNotNullOrderByItemCreatedAtDesc(userId))
                .thenReturn(List.of(archived));

        var items = notificationInboxService.listInbox(userId);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).id()).isEqualTo(archivedId);
        assertThat(items.get(0).read()).isTrue();
        assertThat(items.get(0).body()).contains("Lan mời bạn");
    }

    @Test
    @DisplayName("markAsRead marks only the targeted item when multiple invitations exist")
    void markAsRead_onlyMarksTargetItem() {
        UUID userId = UUID.randomUUID();
        UUID profileInviteId1 = UUID.randomUUID();
        UUID profileInviteId2 = UUID.randomUUID();
        String itemId1 = "PROFILE_INVITATION:" + profileInviteId1;
        String itemId2 = "PROFILE_INVITATION:" + profileInviteId2;

        when(profileShareService.listIncomingInvitations(userId))
                .thenReturn(List.of(
                        new IncomingProfileInvitationResponse(
                                profileInviteId1,
                                UUID.randomUUID(),
                                "Hồ sơ A",
                                "Lan",
                                Instant.parse("2026-01-02T11:00:00Z"),
                                Instant.parse("2026-01-02T10:00:00Z"),
                                "view",
                                "/invitations/accept?token=a"),
                        new IncomingProfileInvitationResponse(
                                profileInviteId2,
                                UUID.randomUUID(),
                                "Hồ sơ B",
                                "Minh",
                                Instant.parse("2026-01-01T11:00:00Z"),
                                Instant.parse("2026-01-01T10:00:00Z"),
                                "view",
                                "/invitations/accept?token=b")));
        when(healthRecordShareService.listIncomingInvitations(userId)).thenReturn(List.of());
        when(readStateRepository.findByUserIdAndInboxItemId(userId, itemId1))
                .thenReturn(java.util.Optional.empty());
        when(readStateRepository.findReadInboxItemIds(eq(userId), any()))
                .thenAnswer(invocation -> {
                    Collection<String> ids = invocation.getArgument(1);
                    if (ids.contains(itemId1)) {
                        return List.of(itemId1);
                    }
                    return List.of();
                });

        notificationInboxService.markAsRead(userId, itemId1);

        var items = notificationInboxService.listInbox(userId);
        assertThat(items).hasSize(2);
        assertThat(items.stream().filter(item -> item.id().equals(itemId1)).findFirst().orElseThrow().read())
                .isTrue();
        assertThat(items.stream().filter(item -> item.id().equals(itemId2)).findFirst().orElseThrow().read())
                .isFalse();
    }

    @Test
    @DisplayName("markAsRead rejects unknown inbox item id")
    void markAsRead_unknownItem_throws() {
        UUID userId = UUID.randomUUID();
        when(profileShareService.listIncomingInvitations(userId)).thenReturn(List.of());
        when(healthRecordShareService.listIncomingInvitations(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> notificationInboxService.markAsRead(userId, "PROFILE_INVITATION:missing"))
                .isInstanceOf(com.healthlens.api.exception.ResourceNotFoundException.class);
    }
}
