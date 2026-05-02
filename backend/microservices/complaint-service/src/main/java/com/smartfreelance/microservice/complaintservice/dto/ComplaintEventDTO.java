package com.smartfreelance.microservice.complaintservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Représentation publique d'un événement du cycle de vie d'une réclamation.
 * Utilisé par GET /api/complaints/{id}/events
 */
@Data
@Builder
public class ComplaintEventDTO {
    private String id;
    private String complaintId;
    private String ticketNumber;
    private String actorId;
    private String actorRole;
    private String eventType;
    private String oldValue;
    private String newValue;
    private String comment;
    private LocalDateTime occurredAt;

    /** Label français de l'eventType (calculé à la construction du DTO) */
    private String eventLabel;

    /** Icône Material suggérée pour la timeline frontend */
    private String icon;
}
