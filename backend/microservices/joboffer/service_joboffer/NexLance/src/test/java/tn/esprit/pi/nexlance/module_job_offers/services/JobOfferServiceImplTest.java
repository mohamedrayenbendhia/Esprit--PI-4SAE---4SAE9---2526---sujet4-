package tn.esprit.pi.nexlance.module_job_offers.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.pi.nexlance.audit.AuditLogService;
import tn.esprit.pi.nexlance.client.RecommendationAiClient;
import tn.esprit.pi.nexlance.module_job_offers.entities.JobOffer;
import tn.esprit.pi.nexlance.module_job_offers.enums.*;
import tn.esprit.pi.nexlance.module_job_offers.repositories.JobOfferRepository;
import tn.esprit.pi.nexlance.notification.NotificationService;

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
class JobOfferServiceImplTest {

    @Mock
    private JobOfferRepository jobOfferRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private RecommendationAiClient aiClient;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private JobOfferServiceImpl jobOfferService;

    private JobOffer jobOffer;
    private UUID jobOfferId;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        jobOfferId = UUID.randomUUID();
        clientId = UUID.randomUUID();

        jobOffer = new JobOffer();
        jobOffer.setId(jobOfferId);
        jobOffer.setClientId(clientId);
        jobOffer.setTitle("Java Developer Needed");
        jobOffer.setDescription("Looking for an experienced Java developer");
        jobOffer.setCategory(JobCategory.DEVELOPMENT);
        jobOffer.setBudget(BigDecimal.valueOf(5000));
        jobOffer.setBudgetType(BudgetType.FIXED);
        jobOffer.setStatus(JobOfferStatus.DRAFT);
        jobOffer.setExperienceLevel(ExperienceLevel.INTERMEDIATE);
        jobOffer.setIsRemote(true);
        jobOffer.setViewCount(0);
        jobOffer.setApplicantCount(0);
        jobOffer.setCreatedAt(LocalDateTime.now());
        jobOffer.setUpdatedAt(LocalDateTime.now());

        lenient().when(auditLogService.logAction(anyString(), anyString(), anyString(),
                any(), any(), anyString(), any(), any())).thenReturn(null);
    }

    @Test
    void createJobOffer_Success() {
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(jobOffer);

        JobOffer result = jobOfferService.createJobOffer(jobOffer);

        assertNotNull(result);
        assertEquals("Java Developer Needed", result.getTitle());
        assertEquals(JobCategory.DEVELOPMENT, result.getCategory());
        verify(jobOfferRepository).save(any(JobOffer.class));
        verify(auditLogService).logAction(eq("CREATE"), eq("JOB_OFFER"), anyString(),
                anyString(), eq("CLIENT"), anyString(), any(), any());
    }

    @Test
    void createJobOffer_NoTitle_ThrowsException() {
        jobOffer.setTitle(null);

        assertThrows(IllegalArgumentException.class,
                () -> jobOfferService.createJobOffer(jobOffer));
    }

    @Test
    void createJobOffer_EmptyTitle_ThrowsException() {
        jobOffer.setTitle("   ");

        assertThrows(IllegalArgumentException.class,
                () -> jobOfferService.createJobOffer(jobOffer));
    }

    @Test
    void createJobOffer_NoCategory_ThrowsException() {
        jobOffer.setCategory(null);

        assertThrows(IllegalArgumentException.class,
                () -> jobOfferService.createJobOffer(jobOffer));
    }

    @Test
    void createJobOffer_DefaultValues() {
        jobOffer.setStatus(null);
        jobOffer.setIsRemote(null);
        jobOffer.setViewCount(null);
        jobOffer.setApplicantCount(null);

        when(jobOfferRepository.save(any(JobOffer.class))).thenAnswer(inv -> {
            JobOffer saved = inv.getArgument(0);
            assertEquals(JobOfferStatus.DRAFT, saved.getStatus());
            assertEquals(false, saved.getIsRemote());
            assertEquals(0, saved.getViewCount());
            assertEquals(0, saved.getApplicantCount());
            return saved;
        });

        jobOfferService.createJobOffer(jobOffer);
    }

    @Test
    void getJobOfferById_Success() {
        when(jobOfferRepository.findById(jobOfferId)).thenReturn(Optional.of(jobOffer));

        JobOffer result = jobOfferService.getJobOfferById(jobOfferId);

        assertNotNull(result);
        assertEquals(jobOfferId, result.getId());
    }

    @Test
    void getJobOfferById_NotFound_ThrowsException() {
        when(jobOfferRepository.findById(jobOfferId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> jobOfferService.getJobOfferById(jobOfferId));
    }

    @Test
    void getAllJobOffers_ReturnsList() {
        when(jobOfferRepository.findAll()).thenReturn(List.of(jobOffer));

        List<JobOffer> result = jobOfferService.getAllJobOffers();

        assertEquals(1, result.size());
    }

    @Test
    void getJobOffersByStatus_ReturnsList() {
        when(jobOfferRepository.findByStatus(JobOfferStatus.DRAFT)).thenReturn(List.of(jobOffer));

        List<JobOffer> result = jobOfferService.getJobOffersByStatus(JobOfferStatus.DRAFT);

        assertEquals(1, result.size());
    }

    @Test
    void getJobOffersByClientId_ReturnsList() {
        when(jobOfferRepository.findByClientId(clientId)).thenReturn(List.of(jobOffer));

        List<JobOffer> result = jobOfferService.getJobOffersByClientId(clientId);

        assertEquals(1, result.size());
        assertEquals(clientId, result.get(0).getClientId());
    }

    @Test
    void getActiveJobOffers_ExcludesArchived() {
        when(jobOfferRepository.findByStatusNot(JobOfferStatus.ARCHIVED)).thenReturn(List.of(jobOffer));

        List<JobOffer> result = jobOfferService.getActiveJobOffers();

        assertEquals(1, result.size());
    }

    @Test
    void getJobOffersByCategory_ReturnsList() {
        when(jobOfferRepository.findByCategory(JobCategory.DEVELOPMENT)).thenReturn(List.of(jobOffer));

        List<JobOffer> result = jobOfferService.getJobOffersByCategory(JobCategory.DEVELOPMENT);

        assertEquals(1, result.size());
    }

    @Test
    void getRemoteJobOffers_ReturnsList() {
        when(jobOfferRepository.findByIsRemote(true)).thenReturn(List.of(jobOffer));

        List<JobOffer> result = jobOfferService.getRemoteJobOffers();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsRemote());
    }

    @Test
    void updateJobOffer_Success() {
        JobOffer updateData = new JobOffer();
        updateData.setTitle("Updated Title");
        updateData.setDescription("Updated description");
        updateData.setBudget(BigDecimal.valueOf(8000));

        when(jobOfferRepository.findById(jobOfferId)).thenReturn(Optional.of(jobOffer));
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(jobOffer);

        JobOffer result = jobOfferService.updateJobOffer(jobOfferId, updateData);

        assertNotNull(result);
        verify(jobOfferRepository).save(any(JobOffer.class));
    }

    @Test
    void updateJobOfferStatus_Success() {
        when(jobOfferRepository.findById(jobOfferId)).thenReturn(Optional.of(jobOffer));
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(jobOffer);

        JobOffer result = jobOfferService.updateJobOfferStatus(jobOfferId, JobOfferStatus.OPEN);

        assertNotNull(result);
        verify(jobOfferRepository).save(argThat(jo -> jo.getStatus() == JobOfferStatus.OPEN));
    }

    @Test
    void updateJobOfferStatus_ToOpen_SetsPublishedAt() {
        jobOffer.setPublishedAt(null);

        when(jobOfferRepository.findById(jobOfferId)).thenReturn(Optional.of(jobOffer));
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(jobOffer);

        jobOfferService.updateJobOfferStatus(jobOfferId, JobOfferStatus.OPEN);

        verify(jobOfferRepository).save(argThat(jo -> jo.getPublishedAt() != null));
    }

    @Test
    void deleteJobOffer_Success() {
        when(jobOfferRepository.findById(jobOfferId)).thenReturn(Optional.of(jobOffer));

        jobOfferService.deleteJobOffer(jobOfferId);

        verify(jobOfferRepository).delete(jobOffer);
    }

    @Test
    void archiveJobOffer_Success() {
        when(jobOfferRepository.findById(jobOfferId)).thenReturn(Optional.of(jobOffer));
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(jobOffer);

        JobOffer result = jobOfferService.archiveJobOffer(jobOfferId);

        assertNotNull(result);
        verify(jobOfferRepository).save(argThat(jo -> jo.getStatus() == JobOfferStatus.ARCHIVED));
    }

    @Test
    void incrementViewCount_Success() {
        jobOffer.setViewCount(5);
        when(jobOfferRepository.findById(jobOfferId)).thenReturn(Optional.of(jobOffer));
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(jobOffer);

        jobOfferService.incrementViewCount(jobOfferId);

        verify(jobOfferRepository).save(argThat(jo -> jo.getViewCount() == 6));
    }

    @Test
    void incrementApplicantCount_Success() {
        jobOffer.setApplicantCount(3);
        when(jobOfferRepository.findById(jobOfferId)).thenReturn(Optional.of(jobOffer));
        when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(jobOffer);

        jobOfferService.incrementApplicantCount(jobOfferId);

        verify(jobOfferRepository).save(argThat(jo -> jo.getApplicantCount() == 4));
    }

    @Test
    void getJobOfferStats_ReturnsCorrectStats() {
        JobOffer offer1 = new JobOffer();
        offer1.setApplicantCount(5);
        JobOffer offer2 = new JobOffer();
        offer2.setApplicantCount(0);

        when(jobOfferRepository.findAll()).thenReturn(List.of(offer1, offer2));

        var stats = jobOfferService.getJobOfferStats();

        assertNotNull(stats);
    }
}
