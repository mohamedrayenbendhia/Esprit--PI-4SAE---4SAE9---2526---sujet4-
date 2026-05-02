package com.smartfreelance.microservice.organizationservice.notification;

public interface OrgNotificationService {
    void notifyMemberAdded(String orgId, String userId, String orgName);
    void notifyMemberRemoved(String orgId, String userId, String orgName);
    void notifyInvitationSent(String orgId, String inviteeId, String orgName);
    void notifyApplicationReceived(String orgId, String ownerId, String applicantId);
    void notifyApplicationResponded(String applicantId, String orgName, boolean accepted);
    void notifyOrganizationVerified(String ownerId, String orgName);
    void notifyOrganizationSuspended(String ownerId, String orgName, String reason);

    /** Avertissement de dormance : l'organisation n'a aucune activité depuis {days} jours. */
    void notifyDormancyWarning(String ownerId, String orgName, long inactiveDays);

    /** Notification que l'offre de collaboration a été fermée automatiquement. */
    void notifyCollabOfferAutoExpired(String ownerId, String orgName, String offerTitle);
}
