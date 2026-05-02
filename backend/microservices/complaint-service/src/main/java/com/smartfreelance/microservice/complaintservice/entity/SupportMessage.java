package com.smartfreelance.microservice.complaintservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entité représentant un message dans une réclamation.
 *
 * Modification : ajout du champ conversationType pour séparer
 * les deux espaces d'échange :
 *   - COMPLAINANT : plaignant ↔ support (toujours créée)
 *   - REPORTED    : partie mise en cause ↔ support (créée sur action explicite)
 */
@Entity
@Table(name = "support_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "complaint_id", nullable = false, length = 36)
    private String complaintId;

    @Column(name = "sender_id", nullable = false, length = 36)
    private String senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private SenderType senderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private MessageType messageType = MessageType.TEXT;

    /**
     * Espace de conversation auquel appartient ce message.
     * COMPLAINANT = fil plaignant ↔ support
     * REPORTED    = fil partie mise en cause ↔ support
     *
     * Par défaut COMPLAINANT pour ne pas casser les messages existants.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false)
    @Builder.Default
    private ConversationType conversationType = ConversationType.COMPLAINANT;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "attachments", columnDefinition = "LONGTEXT")
    private List<String> attachments;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // =====================================================================
    // ENUMS
    // =====================================================================

    public enum SenderType {
        USER,    // Plaignant ou partie mise en cause
        SUPPORT, // Agent de support ou admin
        SYSTEM   // Message automatique du système
    }

    public enum MessageType {
        TEXT,
        NOTE_INTERNE,  // Visible seulement par support/admin
        RESOLUTION,
        AUTO_RESPONSE
    }

    public enum ConversationType {
        /**
         * Fil principal : plaignant ↔ support.
         * Accessible par : reporterId + support/admin.
         */
        COMPLAINANT,

        /**
         * Fil secondaire : partie mise en cause ↔ support.
         * Accessible par : reportedUserId + support/admin.
         * Créé uniquement quand le support active "Impliquer la partie".
         */
        REPORTED
    }
}