package tn.esprit.pi.nexlance.module_job_offers.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nexlance.audit.AuditLogService;
import tn.esprit.pi.nexlance.client.RecommendationAiClient; // Import du client Feign
import tn.esprit.pi.nexlance.module_job_offers.dto.*;
import tn.esprit.pi.nexlance.module_job_offers.entities.JobOffer;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobCategory;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobOfferStatus;
import tn.esprit.pi.nexlance.module_job_offers.enums.ExperienceLevel;
import tn.esprit.pi.nexlance.module_job_offers.repositories.JobOfferRepository;
import tn.esprit.pi.nexlance.notification.NotificationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class JobOfferServiceImpl implements JobOfferService {

    private final JobOfferRepository jobOfferRepository;
    private final AuditLogService auditLogService;
    private final RecommendationAiClient aiClient; // Ajout du client IA
    private final NotificationService notificationService;

    @Autowired
    public JobOfferServiceImpl(JobOfferRepository jobOfferRepository,
                               AuditLogService auditLogService,
                               RecommendationAiClient aiClient,
                               NotificationService notificationService) { // Injection ici
        this.jobOfferRepository = jobOfferRepository;
        this.auditLogService = auditLogService;
        this.aiClient = aiClient;
        this.notificationService = notificationService;
    }

    @Override
    public JobOffer createJobOffer(JobOffer jobOffer) {
        if (jobOffer.getTitle() == null || jobOffer.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (jobOffer.getCategory() == null) {
            throw new IllegalArgumentException("Category is required");
        }
        if (jobOffer.getStatus() == null) {
            jobOffer.setStatus(JobOfferStatus.DRAFT);
        }
        if (jobOffer.getIsRemote() == null) {
            jobOffer.setIsRemote(false);
        }
        if (jobOffer.getViewCount() == null) {
            jobOffer.setViewCount(0);
        }
        if (jobOffer.getApplicantCount() == null) {
            jobOffer.setApplicantCount(0);
        }

        // 1. Sauvegarde dans la base MySQL (Java)
        JobOffer saved = jobOfferRepository.save(jobOffer);

        // 2. SYNCHRONISATION AVEC LE SERVICE IA (Python)
        try {
            Map<String, Object> syncData = new HashMap<>();
            syncData.put("id", saved.getId().toString());
            syncData.put("requiredSkills", saved.getRequiredSkills());
            syncData.put("budget", saved.getBudget() != null ? saved.getBudget().doubleValue() : 0.0);
            syncData.put("experienceRequired", 2); // Valeur par défaut ou extraite de ExperienceLevel
            syncData.put("category", saved.getCategory().name());

            aiClient.syncJobToAi(syncData);
            System.out.println("✅ Synchronisation réussie avec Recommendation-AI");
        } catch (Exception e) {
            // On log l'erreur mais on ne bloque pas la création du job
            System.err.println("❌ Erreur de synchro AI : " + e.getMessage());
        }

        auditLogService.logAction("CREATE", "JOB_OFFER", saved.getId().toString(),
                saved.getClientId() != null ? saved.getClientId().toString() : null, "CLIENT",
                "Created job offer: " + saved.getTitle(), null, null);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public JobOffer getJobOfferById(UUID id) {
        return jobOfferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("JobOffer not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobOffer> getAllJobOffers() {
        return jobOfferRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobOffer> getJobOffersByStatus(JobOfferStatus status) {
        return jobOfferRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobOffer> getJobOffersByClientId(UUID clientId) {
        return jobOfferRepository.findByClientId(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobOffer> getActiveJobOffers() {
        return jobOfferRepository.findByStatusNot(JobOfferStatus.ARCHIVED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobOffer> getJobOffersByCategory(JobCategory category) {
        return jobOfferRepository.findByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobOffer> getRemoteJobOffers() {
        return jobOfferRepository.findByIsRemote(true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobOffer> getJobOffersByExperienceLevel(ExperienceLevel experienceLevel) {
        return jobOfferRepository.findByExperienceLevel(experienceLevel);
    }

    @Override
    public JobOffer updateJobOffer(UUID id, JobOffer jobOffer) {
        JobOffer existingJobOffer = getJobOfferById(id);

        if (jobOffer.getTitle() != null) {
            existingJobOffer.setTitle(jobOffer.getTitle());
        }
        if (jobOffer.getDescription() != null) {
            existingJobOffer.setDescription(jobOffer.getDescription());
        }
        if (jobOffer.getCategory() != null) {
            existingJobOffer.setCategory(jobOffer.getCategory());
        }
        if (jobOffer.getBudget() != null) {
            existingJobOffer.setBudget(jobOffer.getBudget());
        }
        if (jobOffer.getBudgetType() != null) {
            existingJobOffer.setBudgetType(jobOffer.getBudgetType());
        }
        if (jobOffer.getEstimatedDuration() != null) {
            existingJobOffer.setEstimatedDuration(jobOffer.getEstimatedDuration());
        }
        if (jobOffer.getDeadline() != null) {
            existingJobOffer.setDeadline(jobOffer.getDeadline());
        }
        if (jobOffer.getRequiredSkills() != null) {
            existingJobOffer.setRequiredSkills(jobOffer.getRequiredSkills());
        }
        if (jobOffer.getExperienceLevel() != null) {
            existingJobOffer.setExperienceLevel(jobOffer.getExperienceLevel());
        }
        if (jobOffer.getLocation() != null) {
            existingJobOffer.setLocation(jobOffer.getLocation());
        }
        if (jobOffer.getIsRemote() != null) {
            existingJobOffer.setIsRemote(jobOffer.getIsRemote());
        }
        if (jobOffer.getAttachments() != null) {
            existingJobOffer.setAttachments(jobOffer.getAttachments());
        }

        JobOffer updated = jobOfferRepository.save(existingJobOffer);
        auditLogService.logAction("UPDATE", "JOB_OFFER", updated.getId().toString(),
                updated.getClientId() != null ? updated.getClientId().toString() : null, "CLIENT",
                "Updated job offer: " + updated.getTitle(), null, null);
        return updated;
    }

    @Override
    public JobOffer updateJobOfferStatus(UUID id, JobOfferStatus status) {
        JobOffer jobOffer = getJobOfferById(id);
        String oldStatus = jobOffer.getStatus() != null ? jobOffer.getStatus().name() : "null";
        jobOffer.setStatus(status);

        if (status == JobOfferStatus.OPEN && jobOffer.getPublishedAt() == null) {
            jobOffer.setPublishedAt(LocalDateTime.now());
        }

        JobOffer updated = jobOfferRepository.save(jobOffer);
        auditLogService.logAction("STATUS_CHANGE", "JOB_OFFER", updated.getId().toString(),
                updated.getClientId() != null ? updated.getClientId().toString() : null, "CLIENT",
                "Job offer status changed: " + oldStatus + " → " + status.name(),
                oldStatus, status.name());
        if (updated.getClientId() != null) {
            notificationService.notifyJobOfferStatusChanged(
                    updated.getClientId().toString(),
                    updated.getId().toString(),
                    updated.getTitle(),
                    status.name()
            );
        }
        return updated;
    }

    @Override
    public void deleteJobOffer(UUID id) {
        JobOffer jobOffer = getJobOfferById(id);
        auditLogService.logAction("DELETE", "JOB_OFFER", id.toString(),
                jobOffer.getClientId() != null ? jobOffer.getClientId().toString() : null, "CLIENT",
                "Deleted job offer: " + jobOffer.getTitle(), null, null);
        jobOfferRepository.delete(jobOffer);
    }

    @Override
    public JobOffer archiveJobOffer(UUID id) {
        return updateJobOfferStatus(id, JobOfferStatus.ARCHIVED);
    }

    @Override
    public JobOffer incrementViewCount(UUID id) {
        JobOffer jobOffer = getJobOfferById(id);
        jobOffer.setViewCount(jobOffer.getViewCount() + 1);
        return jobOfferRepository.save(jobOffer);
    }

    @Override
    public JobOffer incrementApplicantCount(UUID id) {
        JobOffer jobOffer = getJobOfferById(id);
        jobOffer.setApplicantCount(jobOffer.getApplicantCount() + 1);
        return jobOfferRepository.save(jobOffer);
    }

    @Override
    @Transactional(readOnly = true)
    public JobOfferStatsDTO getJobOfferStats() {
        List<JobOffer> allOffers = jobOfferRepository.findAll();
        long totalOffers = allOffers.size();
        long totalApplications = allOffers.stream()
                .mapToLong(offer -> offer.getApplicantCount() != null ? offer.getApplicantCount() : 0)
                .sum();
        double avgApplicationsPerOffer = totalOffers > 0 ? (double) totalApplications / totalOffers : 0.0;
        long offersWithApplications = allOffers.stream()
                .filter(offer -> offer.getApplicantCount() != null && offer.getApplicantCount() > 0)
                .count();
        double conversionRate = totalOffers > 0 ? (double) offersWithApplications / totalOffers * 100 : 0.0;
        return new JobOfferStatsDTO(totalOffers, totalApplications, conversionRate, avgApplicationsPerOffer, 0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDistributionDTO> getCategoryDistribution() {
        List<JobOffer> allOffers = jobOfferRepository.findAll();
        Map<JobCategory, Long> categoryCounts = allOffers.stream()
                .filter(offer -> offer.getCategory() != null)
                .collect(Collectors.groupingBy(JobOffer::getCategory, Collectors.counting()));

        return categoryCounts.entrySet().stream()
                .map(entry -> new CategoryDistributionDTO(entry.getKey().name(), entry.getValue(), "#3498db"))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetDistributionDTO> getBudgetDistribution() {
        List<JobOffer> allOffers = jobOfferRepository.findAll();
        Map<String, Long> budgetRanges = new LinkedHashMap<>();
        budgetRanges.put("0-500 DT", 0L);
        budgetRanges.put("500-1000 DT", 0L);
        budgetRanges.put("1000-2000 DT", 0L);
        budgetRanges.put("2000-5000 DT", 0L);
        budgetRanges.put("5000+ DT", 0L);

        for (JobOffer offer : allOffers) {
            if (offer.getBudget() != null) {
                BigDecimal budget = offer.getBudget();
                if (budget.compareTo(BigDecimal.valueOf(500)) < 0) budgetRanges.put("0-500 DT", budgetRanges.get("0-500 DT") + 1);
                else if (budget.compareTo(BigDecimal.valueOf(1000)) < 0) budgetRanges.put("500-1000 DT", budgetRanges.get("500-1000 DT") + 1);
                else if (budget.compareTo(BigDecimal.valueOf(2000)) < 0) budgetRanges.put("1000-2000 DT", budgetRanges.get("1000-2000 DT") + 1);
                else if (budget.compareTo(BigDecimal.valueOf(5000)) < 0) budgetRanges.put("2000-5000 DT", budgetRanges.get("2000-5000 DT") + 1);
                else budgetRanges.put("5000+ DT", budgetRanges.get("5000+ DT") + 1);
            }
        }
        return budgetRanges.entrySet().stream().map(e -> new BudgetDistributionDTO(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopClientDTO> getTopClients(int limit) {
        Map<UUID, Long> clientOfferCounts = jobOfferRepository.findAll().stream()
                .collect(Collectors.groupingBy(JobOffer::getClientId, Collectors.counting()));
        return clientOfferCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> new TopClientDTO(e.getKey().toString(), "Client " + e.getKey().toString().substring(0, 8), e.getValue(), 90))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyDataDTO> getMonthlyData(int months) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, MonthlyDataDTO> monthlyMap = new LinkedHashMap<>();
        for (int i = months - 1; i >= 0; i--) {
            String monthKey = now.minusMonths(i).getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            monthlyMap.put(monthKey, new MonthlyDataDTO(monthKey, 0, 0));
        }
        return new ArrayList<>(monthlyMap.values());
    }
}
