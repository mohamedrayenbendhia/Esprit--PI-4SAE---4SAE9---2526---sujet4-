package com.smartfreelance.microservice.notificationservice.repository;

import com.smartfreelance.microservice.notificationservice.entity.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest — teste les requêtes personnalisées du NotificationRepository
 * sur une base H2 en mémoire configurée dans src/test/resources/application.yaml.
 */
@DataJpaTest
@TestPropertySource(properties = "spring.config.import=optional:configserver:")
@DisplayName("NotificationRepository — custom query tests")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository repository;

    private Notification unread1;
    private Notification unread2;
    private Notification read1;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        unread1 = repository.save(Notification.builder()
                .recipientId("user-A")
                .type("INFO")
                .title("Msg 1")
                .message("Body 1")
                .read(false)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build());

        unread2 = repository.save(Notification.builder()
                .recipientId("user-A")
                .type("ALERT")
                .title("Msg 2")
                .message("Body 2")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build());

        read1 = repository.save(Notification.builder()
                .recipientId("user-A")
                .type("INFO")
                .title("Read msg")
                .message("Already read")
                .read(true)
                .readAt(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());

        // notification for another user — should not appear in user-A queries
        repository.save(Notification.builder()
                .recipientId("user-B")
                .type("INFO")
                .title("Other user")
                .message("Should be invisible to user-A")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ── findByRecipientIdOrderByCreatedAtDesc ─────────────────────

    @Test
    @DisplayName("findByRecipientId — retourne toutes les notifs paginées pour l'utilisateur")
    void findByRecipientIdOrderByCreatedAtDesc_shouldReturnAllForUser_orderedDesc() {
        Page<Notification> page = repository.findByRecipientIdOrderByCreatedAtDesc(
                "user-A", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
        // NOTE: @CreationTimestamp is set by Hibernate at persist time, not from the builder.
        // We verify all results belong to user-A and the desc order contract is honoured.
        assertThat(page.getContent()).allMatch(n -> "user-A".equals(n.getRecipientId()));
        // createdAt values must be non-null and in non-ascending order
        List<java.time.LocalDateTime> dates = page.getContent().stream()
                .map(Notification::getCreatedAt).toList();
        for (int i = 0; i < dates.size() - 1; i++) {
            assertThat(dates.get(i)).isAfterOrEqualTo(dates.get(i + 1));
        }
    }

    @Test
    @DisplayName("findByRecipientId — pagination : page 1 de taille 2 retourne 1 élément")
    void findByRecipientIdOrderByCreatedAtDesc_paginated_shouldReturnSlice() {
        Page<Notification> page = repository.findByRecipientIdOrderByCreatedAtDesc(
                "user-A", PageRequest.of(1, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("findByRecipientId — utilisateur inconnu retourne page vide")
    void findByRecipientIdOrderByCreatedAtDesc_unknownUser_shouldReturnEmpty() {
        Page<Notification> page = repository.findByRecipientIdOrderByCreatedAtDesc(
                "user-unknown", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isZero();
    }

    // ── findByRecipientIdAndReadFalseOrderByCreatedAtDesc ─────────

    @Test
    @DisplayName("findUnread — retourne uniquement les non-lues, ordre desc")
    void findByRecipientIdAndReadFalseOrderByCreatedAtDesc_shouldReturnOnlyUnread() {
        List<Notification> unread = repository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc("user-A");

        assertThat(unread).hasSize(2);
        assertThat(unread).allMatch(n -> !n.isRead() && "user-A".equals(n.getRecipientId()));
        // verify desc order (createdAt non-ascending)
        if (unread.size() == 2) {
            assertThat(unread.get(0).getCreatedAt())
                    .isAfterOrEqualTo(unread.get(1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("findUnread — retourne liste vide si toutes les notifs sont lues")
    void findByRecipientIdAndReadFalseOrderByCreatedAtDesc_allRead_shouldReturnEmpty() {
        List<Notification> unread = repository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc("user-B");

        // user-B has 1 unread (the one in setUp), but if we mark it read first:
        repository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc("user-B")
                .forEach(n -> {
                    n.setRead(true);
                    repository.save(n);
                });

        List<Notification> afterMark = repository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc("user-B");
        assertThat(afterMark).isEmpty();
    }

    // ── countByRecipientIdAndReadFalse ────────────────────────────

    @Test
    @DisplayName("countUnread — retourne le bon nombre de non-lues")
    void countByRecipientIdAndReadFalse_shouldReturnCorrectCount() {
        long count = repository.countByRecipientIdAndReadFalse("user-A");
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countUnread — retourne 0 si aucune non-lue pour cet utilisateur")
    void countByRecipientIdAndReadFalse_noUnread_shouldReturnZero() {
        long count = repository.countByRecipientIdAndReadFalse("user-unknown");
        assertThat(count).isZero();
    }

    // ── markAllAsReadByRecipientId ────────────────────────────────

    @Test
    @Transactional
    @DisplayName("markAllAsRead — marque toutes les notifs non-lues comme lues")
    void markAllAsReadByRecipientId_shouldMarkAllAndReturnCount() {
        int marked = repository.markAllAsReadByRecipientId("user-A");

        assertThat(marked).isEqualTo(2); // 2 unread for user-A

        long remaining = repository.countByRecipientIdAndReadFalse("user-A");
        assertThat(remaining).isZero();
    }

    @Test
    @Transactional
    @DisplayName("markAllAsRead — retourne 0 si toutes déjà lues")
    void markAllAsReadByRecipientId_alreadyAllRead_shouldReturnZero() {
        // First mark all as read
        repository.markAllAsReadByRecipientId("user-A");

        // Then mark again — should be 0 since none are unread anymore
        int marked = repository.markAllAsReadByRecipientId("user-A");
        assertThat(marked).isZero();
    }

    @Test
    @Transactional
    @DisplayName("markAllAsRead — n'affecte pas les notifs d'un autre utilisateur")
    void markAllAsReadByRecipientId_shouldNotAffectOtherUsers() {
        long userBUnreadBefore = repository.countByRecipientIdAndReadFalse("user-B");

        repository.markAllAsReadByRecipientId("user-A");

        long userBUnreadAfter = repository.countByRecipientIdAndReadFalse("user-B");
        assertThat(userBUnreadAfter).isEqualTo(userBUnreadBefore);
    }
}
