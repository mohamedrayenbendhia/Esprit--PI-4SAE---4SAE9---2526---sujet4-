package com.smartfreelance.microservice.complaintservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Requête envoyée par le support/admin pour impliquer la partie mise en cause
 * dans une réclamation. Déclenche :
 *   1. Création du fil de conversation REPORTED
 *   2. Notification push + email à la partie mise en cause
 */
@Data
public class InvolveReportedRequest {

    /**
     * Message d'invitation envoyé à la partie mise en cause
     * pour lui expliquer pourquoi elle est impliquée.
     */
    @NotBlank(message = "An invitation message is required")
    private String invitationMessage;
}