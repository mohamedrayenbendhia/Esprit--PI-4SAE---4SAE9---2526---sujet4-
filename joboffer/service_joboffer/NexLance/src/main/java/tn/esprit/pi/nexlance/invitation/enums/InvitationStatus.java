package tn.esprit.pi.nexlance.invitation.enums;

public enum InvitationStatus {
    PENDING,    // Invitation envoyée, en attente de réponse
    ACCEPTED,   // Invitation acceptée par le freelance
    DECLINED,   // Invitation refusée par le freelance
    EXPIRED     // Invitation expirée (deadline dépassée)
}
