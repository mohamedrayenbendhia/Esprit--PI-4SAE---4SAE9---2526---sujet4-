package tn.esprit.pi.nexlance.recommendation.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.pi.nexlance.module_job_offers.entities.JobOffer;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobCategory;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobOfferStatus;
import tn.esprit.pi.nexlance.module_job_offers.repositories.JobOfferRepository;
import tn.esprit.pi.nexlance.notification.NotificationService;
import tn.esprit.pi.nexlance.recommendation.entities.Recommendation;
import tn.esprit.pi.nexlance.recommendation.enums.RecommendationStatus;
import tn.esprit.pi.nexlance.recommendation.repositories.RecommendationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationActivityService activityService;

    @Mock
    private JobOfferRepository jobOfferRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private Recommendation recommendation;
    private Long recommendationId;
    private String clientId;
    private String freelanceId;
    private String jobOfferId;

    @BeforeEach
    void setUp() {
        recommendationId = 1L;
        clientId = UUID.randomUUID().toString();
        freelanceId = UUID.randomUUID().toString();
        jobOfferId = UUID.randomUUID().toString();

        recommendation = Recommendation.builder()
                .id(recommendationId)
                .clientId(clientId)
                .freelanceId(freelanceId)
                .jobOfferId(jobOfferId)
                .message("You would be a great fit for this job")
                .proposedBudget(BigDecimal.valueOf(3000))
                .status(RecommendationStatus.PENDING)
                .viewCount(0)
                .isReminderSent(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .sentDate(LocalDateTime.now())
                .build();
    }

    // ==================== CREATE ====================

    @Test
    void createRecommendation_Success() {
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);
        when(jobOfferRepository.findById(UUID.fromString(jobOfferId))).thenReturn(Optional.empty());

        Recommendation result = recommendationService.createRecommendation(recommendation);

        assertNotNull(result);
        assertEquals(clientId, result.getClientId());
        assertEquals(freelanceId, result.getFreelanceId());
        verify(recommendationRepository).save(any(Recommendation.class));
        verify(activityService).logRecommendationCreated(any(Recommendation.class));
    }

    @Test
    void createRecommendation_SetsDefaultValues() {
        recommendation.setStatus(null);
        recommendation.setViewCount(null);
        recommendation.setIsReminderSent(null);

        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> {
            Recommendation saved = inv.getArgument(0);
            assertEquals(RecommendationStatus.PENDING, saved.getStatus());
            assertEquals(0, saved.getViewCount());
            assertFalse(saved.getIsReminderSent());
            return saved;
        });
        when(jobOfferRepository.findById(UUID.fromString(jobOfferId))).thenReturn(Optional.empty());

        recommendationService.createRecommendation(recommendation);

        verify(recommendationRepository).save(any(Recommendation.class));
    }

    @Test
    void createRecommendation_WithJobOffer_SendsNotification() {
        JobOffer jobOffer = new JobOffer();
        jobOffer.setId(UUID.fromString(jobOfferId));
        jobOffer.setTitle("Java Developer");
        jobOffer.setCategory(JobCategory.DEVELOPMENT);

        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);
        when(jobOfferRepository.findById(UUID.fromString(jobOfferId))).thenReturn(Optional.of(jobOffer));

        recommendationService.createRecommendation(recommendation);

        verify(notificationService).notifyNewRecommendation(
                eq(freelanceId), anyString(), eq("Java Developer"), anyString());
    }

    // ==================== UPDATE ====================

    @Test
    void updateRecommendation_Success() {
        Recommendation updateData = Recommendation.builder()
                .message("Updated message")
                .proposedBudget(BigDecimal.valueOf(5000))
                .expirationDate(LocalDateTime.now().plusDays(30))
                .build();

        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);

        Recommendation result = recommendationService.updateRecommendation(recommendationId, updateData);

        assertNotNull(result);
        verify(recommendationRepository).save(argThat(r ->
                "Updated message".equals(r.getMessage()) &&
                r.getProposedBudget().compareTo(BigDecimal.valueOf(5000)) == 0));
        verify(activityService).logRecommendationUpdated(any(Recommendation.class));
    }

    @Test
    void updateRecommendation_NotFound_ThrowsException() {
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> recommendationService.updateRecommendation(recommendationId, recommendation));
    }

    @Test
    void updateRecommendation_PartialUpdate() {
        Recommendation updateData = Recommendation.builder().message("Only message update").build();

        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);

        recommendationService.updateRecommendation(recommendationId, updateData);

        verify(recommendationRepository).save(argThat(r ->
                "Only message update".equals(r.getMessage()) &&
                r.getProposedBudget().compareTo(BigDecimal.valueOf(3000)) == 0));
    }

    // ==================== GET ====================

    @Test
    void getRecommendationById_Success() {
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        Optional<Recommendation> result = recommendationService.getRecommendationById(recommendationId);

        assertTrue(result.isPresent());
        assertEquals(recommendationId, result.get().getId());
    }

    @Test
    void getRecommendationById_NotFound_ReturnsEmpty() {
        when(recommendationRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Recommendation> result = recommendationService.getRecommendationById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllRecommendations_ReturnsList() {
        when(recommendationRepository.findAll()).thenReturn(List.of(recommendation));

        List<Recommendation> result = recommendationService.getAllRecommendations();

        assertEquals(1, result.size());
    }

    // ==================== DELETE ====================

    @Test
    void deleteRecommendation_Success() {
        recommendationService.deleteRecommendation(recommendationId);

        verify(recommendationRepository).deleteById(recommendationId);
    }

    // ==================== QUERY ====================

    @Test
    void getRecommendationsByClientId_ReturnsList() {
        when(recommendationRepository.findByClientId(clientId)).thenReturn(List.of(recommendation));

        List<Recommendation> result = recommendationService.getRecommendationsByClientId(clientId);

        assertEquals(1, result.size());
        assertEquals(clientId, result.get(0).getClientId());
    }

    @Test
    void getRecommendationsByFreelanceId_ReturnsList() {
        when(recommendationRepository.findByFreelanceId(freelanceId)).thenReturn(List.of(recommendation));

        List<Recommendation> result = recommendationService.getRecommendationsByFreelanceId(freelanceId);

        assertEquals(1, result.size());
        assertEquals(freelanceId, result.get(0).getFreelanceId());
    }

    @Test
    void getRecommendationsByJobOfferId_ReturnsList() {
        when(recommendationRepository.findByJobOfferId(jobOfferId)).thenReturn(List.of(recommendation));

        List<Recommendation> result = recommendationService.getRecommendationsByJobOfferId(jobOfferId);

        assertEquals(1, result.size());
    }

    @Test
    void getRecommendationsByStatus_ReturnsList() {
        when(recommendationRepository.findByStatus(RecommendationStatus.PENDING))
                .thenReturn(List.of(recommendation));

        List<Recommendation> result = recommendationService.getRecommendationsByStatus(RecommendationStatus.PENDING);

        assertEquals(1, result.size());
        assertEquals(RecommendationStatus.PENDING, result.get(0).getStatus());
    }

    @Test
    void getPendingRecommendationsForFreelance_ReturnsList() {
        when(recommendationRepository.findPendingRecommendationsForFreelance(freelanceId))
                .thenReturn(List.of(recommendation));

        List<Recommendation> result = recommendationService.getPendingRecommendationsForFreelance(freelanceId);

        assertEquals(1, result.size());
    }

    // ==================== ACCEPT ====================

    @Test
    void acceptRecommendation_Success() {
        recommendation.setStatus(RecommendationStatus.PENDING);
        recommendation.setExpirationDate(LocalDateTime.now().plusDays(7));

        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);
        when(jobOfferRepository.findById(UUID.fromString(jobOfferId))).thenReturn(Optional.empty());

        Recommendation result = recommendationService.acceptRecommendation(recommendationId, "I accept");

        assertNotNull(result);
        verify(recommendationRepository).save(argThat(r ->
                r.getStatus() == RecommendationStatus.ACCEPTED &&
                r.getResponseDate() != null));
        verify(activityService).logRecommendationAccepted(any(Recommendation.class), eq("I accept"));
    }

    @Test
    void acceptRecommendation_UpdatesJobOfferStatus() {
        recommendation.setStatus(RecommendationStatus.PENDING);
        recommendation.setExpirationDate(LocalDateTime.now().plusDays(7));

        JobOffer jobOffer = new JobOffer();
        jobOffer.setId(UUID.fromString(jobOfferId));
        jobOffer.setTitle("Test Job");
        jobOffer.setCategory(JobCategory.DEVELOPMENT);
        jobOffer.setStatus(JobOfferStatus.OPEN);

        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);
        when(jobOfferRepository.findById(UUID.fromString(jobOfferId))).thenReturn(Optional.of(jobOffer));
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(jobOffer);

        recommendationService.acceptRecommendation(recommendationId, "Accepted");

        verify(jobOfferRepository).save(argThat(jo -> jo.getStatus() == JobOfferStatus.IN_PROGRESS));
    }

    @Test
    void acceptRecommendation_NotFound_ThrowsException() {
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> recommendationService.acceptRecommendation(recommendationId, "response"));
    }

    @Test
    void acceptRecommendation_AlreadyAccepted_ThrowsException() {
        recommendation.setStatus(RecommendationStatus.ACCEPTED);

        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        assertThrows(RuntimeException.class,
                () -> recommendationService.acceptRecommendation(recommendationId, "response"));
    }

    @Test
    void acceptRecommendation_Expired_ThrowsException() {
        recommendation.setStatus(RecommendationStatus.PENDING);
        recommendation.setExpirationDate(LocalDateTime.now().minusDays(1));

        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        assertThrows(RuntimeException.class,
                () -> recommendationService.acceptRecommendation(recommendationId, "response"));
    }

    // ==================== REJECT ====================

    @Test
    void rejectRecommendation_Success() {
        recommendation.setStatus(RecommendationStatus.PENDING);

        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);

        Recommendation result = recommendationService.rejectRecommendation(recommendationId, "Not interested");

        assertNotNull(result);
        verify(recommendationRepository).save(argThat(r ->
                r.getStatus() == RecommendationStatus.REJECTED &&
                r.getResponseDate() != null &&
                "Not interested".equals(r.getCancellationReason())));
        verify(activityService).logRecommendationRejected(any(Recommendation.class), eq("Not interested"));
    }

    @Test
    void rejectRecommendation_NotPending_ThrowsException() {
        recommendation.setStatus(RecommendationStatus.ACCEPTED);

        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        assertThrows(RuntimeException.class,
                () -> recommendationService.rejectRecommendation(recommendationId, "reason"));
    }

    @Test
    void rejectRecommendation_NotFound_ThrowsException() {
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> recommendationService.rejectRecommendation(recommendationId, "reason"));
    }

    // ==================== CANCEL ====================

    @Test
    void cancelRecommendation_Success() {
        recommendation.setStatus(RecommendationStatus.PENDING);

        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);

        Recommendation result = recommendationService.cancelRecommendation(recommendationId, "Changed my mind");

        assertNotNull(result);
        verify(recommendationRepository).save(argThat(r ->
                r.getStatus() == RecommendationStatus.CANCELLED &&
                "Changed my mind".equals(r.getCancellationReason())));
        verify(activityService).logRecommendationCancelled(any(Recommendation.class), eq("Changed my mind"));
    }

    @Test
    void cancelRecommendation_NotPending_ThrowsException() {
        recommendation.setStatus(RecommendationStatus.ACCEPTED);

        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        assertThrows(RuntimeException.class,
                () -> recommendationService.cancelRecommendation(recommendationId, "reason"));
    }

    // ==================== VIEW TRACKING ====================

    @Test
    void incrementViews_Success() {
        recommendation.setViewCount(5);

        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);

        recommendationService.incrementViews(recommendationId);

        verify(recommendationRepository).save(argThat(r -> r.getViewCount() == 6));
        verify(activityService).logRecommendationViewed(any(Recommendation.class));
    }

    @Test
    void incrementViews_NotFound_ThrowsException() {
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> recommendationService.incrementViews(recommendationId));
    }

    // ==================== REMINDER ====================

    @Test
    void sendReminder_Success() {
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);

        Recommendation result = recommendationService.sendReminder(recommendationId);

        assertNotNull(result);
        verify(recommendationRepository).save(argThat(r ->
                r.getIsReminderSent() && r.getReminderSentDate() != null));
        verify(activityService).logReminderSent(any(Recommendation.class));
    }

    @Test
    void sendReminder_NotFound_ThrowsException() {
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> recommendationService.sendReminder(recommendationId));
    }

    // ==================== EXPIRATION ====================

    @Test
    void findAndExpireOldRecommendations_ExpiresAll() {
        Recommendation expired1 = Recommendation.builder()
                .id(2L).clientId(clientId).freelanceId(freelanceId).jobOfferId(jobOfferId)
                .status(RecommendationStatus.PENDING).viewCount(0).isReminderSent(false)
                .expirationDate(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        Recommendation expired2 = Recommendation.builder()
                .id(3L).clientId(clientId).freelanceId(freelanceId).jobOfferId(jobOfferId)
                .status(RecommendationStatus.PENDING).viewCount(0).isReminderSent(false)
                .expirationDate(LocalDateTime.now().minusDays(2))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(recommendationRepository.findExpiredRecommendations())
                .thenReturn(List.of(expired1, expired2));
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Recommendation> result = recommendationService.findAndExpireOldRecommendations();

        assertEquals(2, result.size());
        verify(recommendationRepository, times(2)).save(argThat(r ->
                r.getStatus() == RecommendationStatus.EXPIRED));
        verify(activityService, times(2)).logRecommendationExpired(any(Recommendation.class));
    }

    @Test
    void findAndExpireOldRecommendations_NoneExpired_ReturnsEmpty() {
        when(recommendationRepository.findExpiredRecommendations()).thenReturn(List.of());

        List<Recommendation> result = recommendationService.findAndExpireOldRecommendations();

        assertTrue(result.isEmpty());
        verify(recommendationRepository, never()).save(any(Recommendation.class));
    }

    // ==================== EXISTENCE CHECK ====================

    @Test
    void recommendationExists_ReturnsTrue() {
        when(recommendationRepository.existsByClientIdAndFreelanceIdAndJobOfferId(clientId, freelanceId, jobOfferId))
                .thenReturn(true);

        assertTrue(recommendationService.recommendationExists(clientId, freelanceId, jobOfferId));
    }

    @Test
    void recommendationExists_ReturnsFalse() {
        when(recommendationRepository.existsByClientIdAndFreelanceIdAndJobOfferId(clientId, freelanceId, jobOfferId))
                .thenReturn(false);

        assertFalse(recommendationService.recommendationExists(clientId, freelanceId, jobOfferId));
    }

    // ==================== STATISTICS ====================

    @Test
    void countByStatus_ReturnsCount() {
        when(recommendationRepository.countByStatus(RecommendationStatus.PENDING)).thenReturn(10L);

        long count = recommendationService.countByStatus(RecommendationStatus.PENDING);

        assertEquals(10L, count);
    }

    @Test
    void countByFreelanceIdAndStatus_ReturnsCount() {
        when(recommendationRepository.countByFreelanceIdAndStatus(freelanceId, RecommendationStatus.ACCEPTED))
                .thenReturn(3L);

        long count = recommendationService.countByFreelanceIdAndStatus(freelanceId, RecommendationStatus.ACCEPTED);

        assertEquals(3L, count);
    }

    @Test
    void countByClientIdAndStatus_ReturnsCount() {
        when(recommendationRepository.countByClientIdAndStatus(clientId, RecommendationStatus.PENDING))
                .thenReturn(5L);

        long count = recommendationService.countByClientIdAndStatus(clientId, RecommendationStatus.PENDING);

        assertEquals(5L, count);
    }

    // ==================== RECENT & POPULAR ====================

    @Test
    void getRecentRecommendations_ReturnsList() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        when(recommendationRepository.findRecentRecommendations(since)).thenReturn(List.of(recommendation));

        List<Recommendation> result = recommendationService.getRecentRecommendations(since);

        assertEquals(1, result.size());
    }

    @Test
    void getTopViewedRecommendations_ReturnsList() {
        recommendation.setViewCount(100);
        when(recommendationRepository.findTopViewedRecommendations()).thenReturn(List.of(recommendation));

        List<Recommendation> result = recommendationService.getTopViewedRecommendations();

        assertEquals(1, result.size());
        assertEquals(100, result.get(0).getViewCount());
    }
}
