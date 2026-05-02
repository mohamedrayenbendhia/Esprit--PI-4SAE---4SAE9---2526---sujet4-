package com.smartfreelance.microservice.complaintservice.repository;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.Complaint.Priority;
import com.smartfreelance.microservice.complaintservice.entity.Complaint.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest — vérifie les requêtes personnalisées de ComplaintRepository
 * sur H2 en mémoire (Flyway désactivé via src/test/resources/application.yaml).
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:"
})
@DisplayName("ComplaintRepository — custom query tests")
class ComplaintRepositoryTest {

    @Autowired
    private ComplaintRepository repository;

    private Complaint openUnassigned;
    private Complaint inProgressAssigned;
    private Complaint resolvedAssigned;
    private Complaint closedAssigned;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        openUnassigned = repository.save(buildComplaint("TICK-001", "reporter-1", null,
                Status.OPEN, Priority.MEDIUM));

        inProgressAssigned = repository.save(buildComplaint("TICK-002", "reporter-2", "agent-1",
                Status.IN_PROGRESS, Priority.HIGH));

        resolvedAssigned = repository.save(buildComplaint("TICK-003", "reporter-1", "agent-1",
                Status.RESOLVED, Priority.LOW));

        closedAssigned = repository.save(buildComplaint("TICK-004", "reporter-3", "agent-2",
                Status.CLOSED, Priority.CRITICAL));
    }

    // ── findByTicketNumber ────────────────────────────────────────

    @Test
    @DisplayName("findByTicketNumber — retourne la plainte correspondante")
    void findByTicketNumber_shouldReturnComplaint() {
        Optional<Complaint> result = repository.findByTicketNumber("TICK-001");
        assertThat(result).isPresent();
        assertThat(result.get().getTicketNumber()).isEqualTo("TICK-001");
    }

    @Test
    @DisplayName("findByTicketNumber — retourne vide pour un ticket inconnu")
    void findByTicketNumber_unknown_shouldReturnEmpty() {
        Optional<Complaint> result = repository.findByTicketNumber("TICK-UNKNOWN");
        assertThat(result).isEmpty();
    }

    // ── existsByTicketNumber ──────────────────────────────────────

    @Test
    @DisplayName("existsByTicketNumber — retourne true pour un ticket existant")
    void existsByTicketNumber_existing_shouldReturnTrue() {
        assertThat(repository.existsByTicketNumber("TICK-002")).isTrue();
    }

    @Test
    @DisplayName("existsByTicketNumber — retourne false pour un ticket inexistant")
    void existsByTicketNumber_nonExisting_shouldReturnFalse() {
        assertThat(repository.existsByTicketNumber("TICK-999")).isFalse();
    }

    // ── findByReporterId ──────────────────────────────────────────

    @Test
    @DisplayName("findByReporterId — retourne les plaintes du rapporteur")
    void findByReporterId_shouldReturnComplaintsForReporter() {
        List<Complaint> results = repository.findByReporterId("reporter-1");
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(c -> c.getReporterId().equals("reporter-1"));
    }

    @Test
    @DisplayName("findByReporterId — retourne liste vide pour rapporteur inconnu")
    void findByReporterId_unknown_shouldReturnEmpty() {
        List<Complaint> results = repository.findByReporterId("reporter-unknown");
        assertThat(results).isEmpty();
    }

    // ── findByStatus ──────────────────────────────────────────────

    @Test
    @DisplayName("findByStatus — retourne les plaintes OPEN")
    void findByStatus_open_shouldReturnOnlyOpen() {
        List<Complaint> results = repository.findByStatus(Status.OPEN);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(Status.OPEN);
    }

    @Test
    @DisplayName("findByStatus — retourne liste vide pour statut sans plainte")
    void findByStatus_escalated_shouldReturnEmpty() {
        List<Complaint> results = repository.findByStatus(Status.ESCALATED);
        assertThat(results).isEmpty();
    }

    // ── findByPriority ────────────────────────────────────────────

    @Test
    @DisplayName("findByPriority — retourne les plaintes HIGH")
    void findByPriority_high_shouldReturnOne() {
        List<Complaint> results = repository.findByPriority(Priority.HIGH);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPriority()).isEqualTo(Priority.HIGH);
    }

    // ── findByAssignedToId ────────────────────────────────────────

    @Test
    @DisplayName("findByAssignedToId — retourne les plaintes d'un agent")
    void findByAssignedToId_shouldReturnAssignedToAgent() {
        List<Complaint> results = repository.findByAssignedToId("agent-1");
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(c -> "agent-1".equals(c.getAssignedToId()));
    }

    // ── findUnassignedByStatus (@Query) ───────────────────────────

    @Test
    @DisplayName("findUnassignedByStatus — retourne les plaintes sans agent assigné")
    void findUnassignedByStatus_open_shouldReturnUnassigned() {
        List<Complaint> results = repository.findUnassignedByStatus(Status.OPEN);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAssignedToId()).isNull();
    }

    @Test
    @DisplayName("findUnassignedByStatus — retourne vide si tous les OPEN sont assignés")
    void findUnassignedByStatus_whenAllAssigned_shouldReturnEmpty() {
        // Assign the open unassigned complaint
        openUnassigned.setAssignedToId("agent-1");
        repository.save(openUnassigned);

        List<Complaint> results = repository.findUnassignedByStatus(Status.OPEN);
        assertThat(results).isEmpty();
    }

    // ── countByStatus ─────────────────────────────────────────────

    @Test
    @DisplayName("countByStatus — retourne le bon nombre de plaintes par statut")
    void countByStatus_shouldReturnCorrectCount() {
        assertThat(repository.countByStatus(Status.OPEN)).isEqualTo(1);
        assertThat(repository.countByStatus(Status.IN_PROGRESS)).isEqualTo(1);
        assertThat(repository.countByStatus(Status.RESOLVED)).isEqualTo(1);
        assertThat(repository.countByStatus(Status.CLOSED)).isEqualTo(1);
        assertThat(repository.countByStatus(Status.ESCALATED)).isZero();
    }

    // ── countByPriority ───────────────────────────────────────────

    @Test
    @DisplayName("countByPriority — retourne le bon nombre de plaintes par priorité")
    void countByPriority_shouldReturnCorrectCount() {
        assertThat(repository.countByPriority(Priority.MEDIUM)).isEqualTo(1);
        assertThat(repository.countByPriority(Priority.HIGH)).isEqualTo(1);
        assertThat(repository.countByPriority(Priority.CRITICAL)).isEqualTo(1);
        assertThat(repository.countByPriority(Priority.LOW)).isEqualTo(1);
    }

    // ── findByDateRange (@Query) ──────────────────────────────────

    @Test
    @DisplayName("findByDateRange — retourne les plaintes dans la plage de dates")
    void findByDateRange_shouldReturnComplaintsInRange() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(10);
        LocalDateTime end   = LocalDateTime.now().plusMinutes(10);

        List<Complaint> results = repository.findByDateRange(start, end);
        assertThat(results).hasSize(4); // all created in setUp within this range
    }

    @Test
    @DisplayName("findByDateRange — retourne vide si aucune plainte dans la plage")
    void findByDateRange_noMatch_shouldReturnEmpty() {
        LocalDateTime start = LocalDateTime.now().minusYears(10);
        LocalDateTime end   = LocalDateTime.now().minusYears(9);

        List<Complaint> results = repository.findByDateRange(start, end);
        assertThat(results).isEmpty();
    }

    // ── findOverdueComplaints (@Query) ────────────────────────────

    @Test
    @DisplayName("findOverdueComplaints — retourne les plaintes OPEN/IN_PROGRESS créées avant le seuil")
    void findOverdueComplaints_shouldReturnNonTerminalOverdueComplaints() {
        // threshold = now → all 4 complaints were created before now, but RESOLVED/CLOSED are excluded
        LocalDateTime threshold = LocalDateTime.now().plusSeconds(1);

        List<Complaint> overdue = repository.findOverdueComplaints(threshold);

        assertThat(overdue).hasSize(2); // OPEN + IN_PROGRESS only (RESOLVED and CLOSED are excluded)
        assertThat(overdue).allMatch(c ->
                c.getStatus() != Status.RESOLVED && c.getStatus() != Status.CLOSED);
    }

    @Test
    @DisplayName("findOverdueComplaints — retourne vide si seuil dans le passé")
    void findOverdueComplaints_futureThreshold_noOverdue_shouldReturnEmpty() {
        // All complaints were created now; threshold is in the past → no overdue
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);

        List<Complaint> overdue = repository.findOverdueComplaints(threshold);
        assertThat(overdue).isEmpty();
    }

    // ── findByStatusAndPriority ───────────────────────────────────

    @Test
    @DisplayName("findByStatusAndPriority — filtre combiné status + priority")
    void findByStatusAndPriority_shouldFilterCorrectly() {
        List<Complaint> results = repository.findByStatusAndPriority(Status.IN_PROGRESS, Priority.HIGH);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTicketNumber()).isEqualTo("TICK-002");
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Complaint buildComplaint(String ticket, String reporterId, String assignedToId,
                                     Status status, Priority priority) {
        return Complaint.builder()
                .ticketNumber(ticket)
                .reporterId(reporterId)
                .assignedToId(assignedToId)
                .status(status)
                .priority(priority)
                .category(Complaint.ComplaintCategory.PAYMENT_ISSUE)
                .subject("Test subject")
                .description("Test description")
                .build();
    }
}
