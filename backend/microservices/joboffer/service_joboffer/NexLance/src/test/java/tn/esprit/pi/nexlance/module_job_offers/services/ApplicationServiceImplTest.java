package tn.esprit.pi.nexlance.module_job_offers.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.pi.nexlance.audit.AuditLogService;
import tn.esprit.pi.nexlance.module_job_offers.entities.Application;
import tn.esprit.pi.nexlance.module_job_offers.entities.JobOffer;
import tn.esprit.pi.nexlance.module_job_offers.enums.ApplicationStatus;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobCategory;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobOfferStatus;
import tn.esprit.pi.nexlance.module_job_offers.repositories.ApplicationRepository;
import tn.esprit.pi.nexlance.notification.NotificationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobOfferService jobOfferService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private Application application;
    private UUID applicationId;
    private UUID jobOfferId;
    private UUID freelanceId;
    private JobOffer jobOffer;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        jobOfferId = UUID.randomUUID();
        freelanceId = UUID.randomUUID();

        application = new Application();
        application.setId(applicationId);
        application.setJobOfferId(jobOfferId);
        application.setFreelanceId(freelanceId);
        application.setCoverLetter("I'm interested in this job");
        application.setProposedRate(BigDecimal.valueOf(45));
        application.setStatus(ApplicationStatus.PENDING);
        application.setIsRead(false);
        application.setCreatedAt(LocalDateTime.now());
        application.setSubmittedAt(LocalDateTime.now());

        jobOffer = new JobOffer();
        jobOffer.setId(jobOfferId);
        jobOffer.setTitle("Test Job");
        jobOffer.setCategory(JobCategory.DEVELOPMENT);
        jobOffer.setStatus(JobOfferStatus.OPEN);
        jobOffer.setApplicantCount(0);

        lenient().when(auditLogService.logAction(anyString(), anyString(), anyString(),
                any(), any(), anyString(), any(), any())).thenReturn(null);
    }

    @Test
    void createApplication_Success() {
        when(jobOfferService.getJobOfferById(jobOfferId)).thenReturn(jobOffer);
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(jobOfferService.incrementApplicantCount(jobOfferId)).thenReturn(jobOffer);

        Application result = applicationService.createApplication(application);

        assertNotNull(result);
        assertEquals(jobOfferId, result.getJobOfferId());
        assertEquals(freelanceId, result.getFreelanceId());
        verify(jobOfferService).incrementApplicantCount(jobOfferId);
    }

    @Test
    void createApplication_NoJobOfferId_ThrowsException() {
        application.setJobOfferId(null);

        assertThrows(IllegalArgumentException.class,
                () -> applicationService.createApplication(application));
    }

    @Test
    void createApplication_NoFreelanceId_ThrowsException() {
        application.setFreelanceId(null);

        assertThrows(IllegalArgumentException.class,
                () -> applicationService.createApplication(application));
    }

    @Test
    void createApplication_DefaultValues() {
        application.setStatus(null);
        application.setIsRead(null);

        when(jobOfferService.getJobOfferById(jobOfferId)).thenReturn(jobOffer);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application saved = inv.getArgument(0);
            assertEquals(ApplicationStatus.PENDING, saved.getStatus());
            assertEquals(false, saved.getIsRead());
            return saved;
        });
        when(jobOfferService.incrementApplicantCount(jobOfferId)).thenReturn(jobOffer);

        applicationService.createApplication(application);
    }

    @Test
    void getApplicationById_Success() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        Application result = applicationService.getApplicationById(applicationId);

        assertNotNull(result);
        assertEquals(applicationId, result.getId());
    }

    @Test
    void getApplicationById_NotFound_ThrowsException() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> applicationService.getApplicationById(applicationId));
    }

    @Test
    void getAllApplications_ReturnsList() {
        when(applicationRepository.findAll()).thenReturn(List.of(application));

        List<Application> result = applicationService.getAllApplications();

        assertEquals(1, result.size());
    }

    @Test
    void getApplicationsByJobOfferId_ReturnsList() {
        when(applicationRepository.findByJobOfferId(jobOfferId)).thenReturn(List.of(application));

        List<Application> result = applicationService.getApplicationsByJobOfferId(jobOfferId);

        assertEquals(1, result.size());
    }

    @Test
    void getApplicationsByFreelanceId_ReturnsList() {
        when(applicationRepository.findByFreelanceId(freelanceId)).thenReturn(List.of(application));

        List<Application> result = applicationService.getApplicationsByFreelanceId(freelanceId);

        assertEquals(1, result.size());
    }

    @Test
    void getApplicationsByStatus_ReturnsList() {
        when(applicationRepository.findByStatus(ApplicationStatus.PENDING)).thenReturn(List.of(application));

        List<Application> result = applicationService.getApplicationsByStatus(ApplicationStatus.PENDING);

        assertEquals(1, result.size());
    }

    @Test
    void getUnreadApplicationsByJobOfferId_ReturnsList() {
        when(applicationRepository.findByJobOfferIdAndIsRead(jobOfferId, false))
                .thenReturn(List.of(application));

        List<Application> result = applicationService.getUnreadApplicationsByJobOfferId(jobOfferId);

        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsRead());
    }

    @Test
    void countApplicationsByJobOfferId_ReturnsCount() {
        when(applicationRepository.countByJobOfferId(jobOfferId)).thenReturn(5L);

        Long count = applicationService.countApplicationsByJobOfferId(jobOfferId);

        assertEquals(5L, count);
    }

    @Test
    void getApplicationCountsByStatus_ReturnsAllCounts() {
        when(applicationRepository.countByJobOfferId(jobOfferId)).thenReturn(10L);
        when(applicationRepository.countByJobOfferIdAndStatus(jobOfferId, ApplicationStatus.PENDING)).thenReturn(3L);
        when(applicationRepository.countByJobOfferIdAndStatus(jobOfferId, ApplicationStatus.SHORTLISTED)).thenReturn(2L);
        when(applicationRepository.countByJobOfferIdAndStatus(jobOfferId, ApplicationStatus.ACCEPTED)).thenReturn(1L);
        when(applicationRepository.countByJobOfferIdAndStatus(jobOfferId, ApplicationStatus.REJECTED)).thenReturn(3L);
        when(applicationRepository.countByJobOfferIdAndStatus(jobOfferId, ApplicationStatus.WITHDRAWN)).thenReturn(1L);

        Map<String, Long> counts = applicationService.getApplicationCountsByStatus(jobOfferId);

        assertEquals(10L, counts.get("total"));
        assertEquals(3L, counts.get("pending"));
        assertEquals(2L, counts.get("shortlisted"));
        assertEquals(1L, counts.get("accepted"));
        assertEquals(3L, counts.get("rejected"));
        assertEquals(1L, counts.get("withdrawn"));
    }

    @Test
    void updateApplication_Success() {
        Application updateData = new Application();
        updateData.setCoverLetter("Updated cover letter");
        updateData.setProposedRate(BigDecimal.valueOf(55));

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);

        Application result = applicationService.updateApplication(applicationId, updateData);

        assertNotNull(result);
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void updateApplicationStatus_Success() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);

        Application result = applicationService.updateApplicationStatus(applicationId, ApplicationStatus.SHORTLISTED);

        assertNotNull(result);
        verify(applicationRepository).save(argThat(a ->
                a.getStatus() == ApplicationStatus.SHORTLISTED));
    }

    @Test
    void updateApplicationStatus_SetsRespondedAt() {
        application.setRespondedAt(null);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);

        applicationService.updateApplicationStatus(applicationId, ApplicationStatus.ACCEPTED);

        verify(applicationRepository).save(argThat(a -> a.getRespondedAt() != null));
    }

    @Test
    void markAsRead_Success() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);

        Application result = applicationService.markAsRead(applicationId);

        assertNotNull(result);
        verify(applicationRepository).save(argThat(a -> a.getIsRead()));
    }

    @Test
    void deleteApplication_Success() {
        application.setStatus(ApplicationStatus.PENDING);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        applicationService.deleteApplication(applicationId);

        verify(applicationRepository).delete(application);
    }

    @Test
    void deleteApplication_AcceptedApplication_ThrowsException() {
        application.setStatus(ApplicationStatus.ACCEPTED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThrows(IllegalStateException.class,
                () -> applicationService.deleteApplication(applicationId));

        verify(applicationRepository, never()).delete(any());
    }
}
