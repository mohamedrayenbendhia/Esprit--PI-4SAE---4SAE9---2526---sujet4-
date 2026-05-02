package com.smartfreelance.microservice.complaintservice.repository;

import com.smartfreelance.microservice.complaintservice.entity.SupportMessage;
import com.smartfreelance.microservice.complaintservice.entity.SupportMessage.SenderType;
import com.smartfreelance.microservice.complaintservice.entity.SupportMessage.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, String> {

    // ── Requêtes existantes ────────────────────────────────────────────────

    List<SupportMessage> findByComplaintIdOrderByCreatedAtAsc(String complaintId);

    List<SupportMessage> findBySenderId(String senderId);

    @Query("SELECT m FROM SupportMessage m WHERE m.complaintId = :complaintId AND m.isRead = false")
    List<SupportMessage> findUnreadByComplaint(@Param("complaintId") String complaintId);

    long countByComplaintIdAndIsReadFalse(String complaintId);

    List<SupportMessage> findBySenderType(SenderType senderType);

    @Query("SELECT m FROM SupportMessage m WHERE m.complaintId = :complaintId " +
            "ORDER BY m.createdAt DESC LIMIT 1")
    SupportMessage findLatestByComplaint(@Param("complaintId") String complaintId);

    // ── Nouvelles requêtes filtrées par conversationType ──────────────────

    /**
     * Messages d'une réclamation filtrés par fil de conversation.
     * Utilisé pour afficher séparément les deux conversations.
     */
    List<SupportMessage> findByComplaintIdAndConversationTypeOrderByCreatedAtAsc(
            String complaintId, ConversationType conversationType);

    /**
     * Vérifie si le fil REPORTED existe déjà pour une réclamation.
     * Évite de créer un doublon lors de l'action "Impliquer la partie".
     */
    boolean existsByComplaintIdAndConversationType(String complaintId, ConversationType conversationType);

    /**
     * Messages non lus d'un fil de conversation spécifique.
     */
    @Query("SELECT m FROM SupportMessage m WHERE m.complaintId = :complaintId " +
            "AND m.conversationType = :type AND m.isRead = false")
    List<SupportMessage> findUnreadByComplaintAndType(
            @Param("complaintId") String complaintId,
            @Param("type") ConversationType type);

    /**
     * Nombre de messages non lus dans un fil spécifique.
     */
    long countByComplaintIdAndConversationTypeAndIsReadFalse(
            String complaintId, ConversationType conversationType);
}