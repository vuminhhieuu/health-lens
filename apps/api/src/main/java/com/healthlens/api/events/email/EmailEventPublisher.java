package com.healthlens.api.events.email;

import com.healthlens.api.entity.DataDeletionRequest;
import com.healthlens.api.entity.User;

import java.util.UUID;

public interface EmailEventPublisher {

    void publishVerification(User user, String token);

    void publishPasswordReset(User user, String token);

    void publishDeletionConfirmation(User user, DataDeletionRequest deletionRequest, String cancellationLink);

    void publishDeletionCancellation(User user);

    void publishDeletionCompletion(User user);

    void publishProfileInvitation(User inviter, String inviteeEmail, String invitationLink);

    /** Notifies the profile owner that an invitee accepted sharing access. */
    void publishProfileShareAccepted(User owner, User viewer, String profileDisplayName, String profilesLink);

    void publishHealthRecordInvitation(User inviter, String inviteeEmail, String invitationLink);

    void publishFollowUpReminder(UUID reminderId);
}
