package com.healthlens.api.service;

import com.healthlens.api.dto.response.IncomingHealthRecordInvitationResponse;
import com.healthlens.api.dto.response.IncomingProfileInvitationResponse;
import com.healthlens.api.dto.response.NotificationInboxItemResponse;
import com.healthlens.api.dto.response.NotificationInboxItemType;
import com.healthlens.api.entity.FollowUpReminder;
import com.healthlens.api.entity.NotificationInboxReadState;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.FollowUpReminderRepository;
import com.healthlens.api.repository.NotificationInboxReadStateRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationInboxService {

    static final int MAX_ITEMS = 50;
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter REMINDER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int UPCOMING_REMINDER_HORIZON_DAYS = 90;

    private final ProfileShareService profileShareService;
    private final HealthRecordShareService healthRecordShareService;
    private final FollowUpReminderRepository followUpReminderRepository;
    private final NotificationInboxReadStateRepository readStateRepository;
    private final FollowUpReminderService followUpReminderService;

    public NotificationInboxService(
            ProfileShareService profileShareService,
            HealthRecordShareService healthRecordShareService,
            FollowUpReminderRepository followUpReminderRepository,
            NotificationInboxReadStateRepository readStateRepository,
            @Lazy FollowUpReminderService followUpReminderService) {
        this.profileShareService = profileShareService;
        this.healthRecordShareService = healthRecordShareService;
        this.followUpReminderRepository = followUpReminderRepository;
        this.readStateRepository = readStateRepository;
        this.followUpReminderService = followUpReminderService;
    }

    @Transactional(readOnly = true)
    public List<NotificationInboxItemResponse> listInbox(UUID userId) {
        followUpReminderService.dispatchDueReminderEmailsIfEnabled(userId);
        List<NotificationInboxItemResponse> activeItems = applyReadState(userId, buildSortedInbox(userId));
        List<NotificationInboxItemResponse> archivedReadItems = loadArchivedReadItems(userId, activeItems);
        return mergeAndCap(activeItems, archivedReadItems);
    }

    @Transactional
    public void markAsRead(UUID userId, String itemId) {
        List<NotificationInboxItemResponse> inbox = buildSortedInbox(userId);
        NotificationInboxItemResponse item = inbox.stream()
                .filter(entry -> entry.id().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Thông báo không tồn tại"));

        readStateRepository
                .findByUserIdAndInboxItemId(userId, itemId)
                .ifPresentOrElse(
                        existing -> applySnapshot(existing, item, Instant.now()),
                        () -> {
                            NotificationInboxReadState state = new NotificationInboxReadState();
                            state.setUserId(userId);
                            state.setInboxItemId(itemId);
                            applySnapshot(state, item, Instant.now());
                            readStateRepository.save(state);
                        });
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        List<NotificationInboxItemResponse> inbox = buildSortedInbox(userId);
        if (inbox.isEmpty()) {
            return 0;
        }

        Set<String> alreadyRead = new HashSet<>(readStateRepository.findReadInboxItemIds(
                userId, inbox.stream().map(NotificationInboxItemResponse::id).toList()));
        int newlyMarked = 0;
        Instant now = Instant.now();

        for (NotificationInboxItemResponse item : inbox) {
            if (alreadyRead.contains(item.id())) {
                continue;
            }
            NotificationInboxReadState state = new NotificationInboxReadState();
            state.setUserId(userId);
            state.setInboxItemId(item.id());
            applySnapshot(state, item, now);
            readStateRepository.save(state);
            newlyMarked++;
        }

        return newlyMarked;
    }

    private List<NotificationInboxItemResponse> loadArchivedReadItems(
            UUID userId, List<NotificationInboxItemResponse> activeItems) {
        Set<String> activeIds =
                activeItems.stream().map(NotificationInboxItemResponse::id).collect(Collectors.toSet());

        List<NotificationInboxReadState> archivedStates = activeIds.isEmpty()
                ? readStateRepository.findByUserIdAndItemTypeIsNotNullOrderByItemCreatedAtDesc(userId)
                : readStateRepository.findByUserIdAndItemTypeIsNotNullAndInboxItemIdNotIn(userId, activeIds);

        return archivedStates.stream().map(this::mapArchivedRead).toList();
    }

    private static List<NotificationInboxItemResponse> mergeAndCap(
            List<NotificationInboxItemResponse> activeItems, List<NotificationInboxItemResponse> archivedReadItems) {
        List<NotificationInboxItemResponse> merged = new ArrayList<>(activeItems.size() + archivedReadItems.size());
        merged.addAll(activeItems);
        merged.addAll(archivedReadItems);
        return merged.stream()
                .sorted(Comparator.comparing(NotificationInboxItemResponse::createdAt).reversed())
                .limit(MAX_ITEMS)
                .toList();
    }

    private NotificationInboxItemResponse mapArchivedRead(NotificationInboxReadState state) {
        NotificationInboxItemType type = NotificationInboxItemType.valueOf(state.getItemType());
        return new NotificationInboxItemResponse(
                state.getInboxItemId(),
                type,
                state.getTitle(),
                state.getBody(),
                state.getItemCreatedAt(),
                state.getActionUrl(),
                true);
    }

    private static void applySnapshot(NotificationInboxReadState state, NotificationInboxItemResponse item) {
        applySnapshot(state, item, Instant.now());
    }

    private static void applySnapshot(
            NotificationInboxReadState state, NotificationInboxItemResponse item, Instant readAt) {
        state.setReadAt(readAt);
        state.setItemType(item.type().name());
        state.setTitle(item.title());
        state.setBody(item.body());
        state.setItemCreatedAt(item.createdAt());
        state.setActionUrl(item.actionUrl());
    }

    private List<NotificationInboxItemResponse> buildSortedInbox(UUID userId) {
        List<NotificationInboxItemResponse> items = new ArrayList<>();

        for (IncomingProfileInvitationResponse invitation : profileShareService.listIncomingInvitations(userId)) {
            items.add(mapProfileInvitation(invitation));
        }
        for (IncomingHealthRecordInvitationResponse invitation :
                healthRecordShareService.listIncomingInvitations(userId)) {
            items.add(mapHealthRecordInvitation(invitation));
        }
        for (FollowUpReminder reminder : listUpcomingReminders(userId)) {
            items.add(mapUpcomingReminder(reminder));
        }

        return items.stream()
                .sorted(Comparator.comparing(NotificationInboxItemResponse::createdAt).reversed())
                .limit(MAX_ITEMS)
                .toList();
    }

    private List<NotificationInboxItemResponse> applyReadState(
            UUID userId, List<NotificationInboxItemResponse> items) {
        if (items.isEmpty()) {
            return items;
        }

        List<String> inboxItemIds = items.stream().map(NotificationInboxItemResponse::id).toList();
        Set<String> readIds = new HashSet<>(readStateRepository.findReadInboxItemIds(userId, inboxItemIds));

        return items.stream()
                .map(item -> new NotificationInboxItemResponse(
                        item.id(),
                        item.type(),
                        item.title(),
                        item.body(),
                        item.createdAt(),
                        item.actionUrl(),
                        readIds.contains(item.id())))
                .collect(Collectors.toList());
    }

    private static NotificationInboxItemResponse mapProfileInvitation(IncomingProfileInvitationResponse invitation) {
        String profileName = invitation.profileDisplayName() != null && !invitation.profileDisplayName().isBlank()
                ? invitation.profileDisplayName()
                : "Hồ sơ";
        String inviterName = invitation.inviterName() != null && !invitation.inviterName().isBlank()
                ? invitation.inviterName()
                : "Một thành viên";
        return new NotificationInboxItemResponse(
                inboxItemId(NotificationInboxItemType.PROFILE_INVITATION, invitation.id()),
                NotificationInboxItemType.PROFILE_INVITATION,
                "Lời mời xem hồ sơ",
                inviterName + " mời bạn xem hồ sơ \"" + profileName + "\".",
                invitation.createdAt(),
                invitation.acceptPath(),
                false);
    }

    private static NotificationInboxItemResponse mapHealthRecordInvitation(
            IncomingHealthRecordInvitationResponse invitation) {
        String inviterName = invitation.inviterName() != null && !invitation.inviterName().isBlank()
                ? invitation.inviterName()
                : "Một người dùng";
        return new NotificationInboxItemResponse(
                inboxItemId(NotificationInboxItemType.HEALTH_RECORD_INVITATION, invitation.id()),
                NotificationInboxItemType.HEALTH_RECORD_INVITATION,
                "Lời mời xem kết quả xét nghiệm",
                inviterName + " mời bạn xem một kết quả xét nghiệm được chia sẻ.",
                invitation.createdAt(),
                invitation.acceptPath(),
                false);
    }

    private List<FollowUpReminder> listUpcomingReminders(UUID userId) {
        LocalDate today = LocalDate.now(VN_ZONE);
        return followUpReminderRepository.findActiveRemindersForInbox(
                userId, today, today.plusDays(UPCOMING_REMINDER_HORIZON_DAYS));
    }

    private static NotificationInboxItemResponse mapUpcomingReminder(FollowUpReminder reminder) {
        LocalDate today = LocalDate.now(VN_ZONE);
        String profileName = reminder.getProfile() != null
                        && reminder.getProfile().getDisplayName() != null
                        && !reminder.getProfile().getDisplayName().isBlank()
                ? reminder.getProfile().getDisplayName().trim()
                : "Hồ sơ sức khỏe";
        String reminderType = reminder.getReminderType() != null && !reminder.getReminderType().isBlank()
                ? reminder.getReminderType().trim()
                : "Tái khám";
        String formattedDate = REMINDER_DATE_FORMAT.format(reminder.getReminderDate());

        String body;
        if (reminder.getReminderDate().isBefore(today)) {
            body = "Quá hạn từ ngày " + formattedDate + ": nhắc " + reminderType + " cho \"" + profileName + "\".";
        } else if (reminder.getReminderDate().isEqual(today)) {
            body = "Hôm nay: nhắc " + reminderType + " cho \"" + profileName + "\".";
        } else {
            body = "Ngày " + formattedDate + ": nhắc " + reminderType + " cho \"" + profileName + "\".";
        }

        UUID profileId = reminder.getProfile() != null ? reminder.getProfile().getId() : null;
        String actionUrl = profileId != null
                ? "/follow-up-reminders?profileId=" + profileId
                : "/follow-up-reminders";

        Instant createdAt = reminder.getUpdatedAt() != null ? reminder.getUpdatedAt() : reminder.getCreatedAt();

        return new NotificationInboxItemResponse(
                inboxItemId(NotificationInboxItemType.REMINDER_UPCOMING, reminder.getId()),
                NotificationInboxItemType.REMINDER_UPCOMING,
                "Nhắc lịch tái khám",
                body,
                createdAt,
                actionUrl,
                false);
    }

    private static String inboxItemId(NotificationInboxItemType type, UUID entityId) {
        return type.name() + ":" + entityId;
    }
}
