package tn.esprit.pi.nexlance.module_job_offers.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nexlance.audit.AuditLogService;
import tn.esprit.pi.nexlance.module_job_offers.dto.*;
import tn.esprit.pi.nexlance.module_job_offers.entities.JobOffer;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobCategory;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobOfferStatus;
import tn.esprit.pi.nexlance.module_job_offers.enums.ExperienceLevel;
import tn.esprit.pi.nexlance.module_job_offers.repositories.JobOfferRepository;

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

    @Autowired
    public JobOfferServiceImpl(JobOfferRepository jobOfferRepository, AuditLogService auditLogService) {
        this.jobOfferRepository = jobOfferRepository;
        this.auditLogService = auditLogService;
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
        JobOffer saved = jobOfferRepository.save(jobOffer);
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
        
        // Calculate average applications per offer
        double avgApplicationsPerOffer = totalOffers > 0 
                ? (double) totalApplications / totalOffers 
                : 0.0;
        
        // Calculate conversion rate (offers with at least one application / total offers)
        long offersWithApplications = allOffers.stream()
                .filter(offer -> offer.getApplicantCount() != null && offer.getApplicantCount() > 0)
                .count();
        double conversionRate = totalOffers > 0 
                ? (double) offersWithApplications / totalOffers * 100 
                : 0.0;
        
        // For avgResponseTime, we would need response timestamps which we don't have yet
        // Setting it to 0 for now
        double avgResponseTime = 0.0;
        
        return new JobOfferStatsDTO(
                totalOffers,
                totalApplications,
                conversionRate,
                avgApplicationsPerOffer,
                avgResponseTime
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDistributionDTO> getCategoryDistribution() {
        List<JobOffer> allOffers = jobOfferRepository.findAll();
        
        Map<String, String> categoryColors = new HashMap<>();
        categoryColors.put("DEVELOPMENT", "#3498db");
        categoryColors.put("DESIGN", "#9b59b6");
        categoryColors.put("MARKETING", "#e74c3c");
        categoryColors.put("WRITING", "#f39c12");
        categoryColors.put("DATA_SCIENCE", "#1abc9c");
        categoryColors.put("CUSTOMER_SERVICE", "#e67e22");
        categoryColors.put("OTHER", "#95a5a6");
        
        Map<JobCategory, Long> categoryCounts = allOffers.stream()
                .filter(offer -> offer.getCategory() != null)
                .collect(Collectors.groupingBy(JobOffer::getCategory, Collectors.counting()));
        
        return categoryCounts.entrySet().stream()
                .map(entry -> new CategoryDistributionDTO(
                        entry.getKey().name(),
                        entry.getValue(),
                        categoryColors.getOrDefault(entry.getKey().name(), "#95a5a6")
                ))
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
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
                if (budget.compareTo(BigDecimal.valueOf(500)) < 0) {
                    budgetRanges.put("0-500 DT", budgetRanges.get("0-500 DT") + 1);
                } else if (budget.compareTo(BigDecimal.valueOf(1000)) < 0) {
                    budgetRanges.put("500-1000 DT", budgetRanges.get("500-1000 DT") + 1);
                } else if (budget.compareTo(BigDecimal.valueOf(2000)) < 0) {
                    budgetRanges.put("1000-2000 DT", budgetRanges.get("1000-2000 DT") + 1);
                } else if (budget.compareTo(BigDecimal.valueOf(5000)) < 0) {
                    budgetRanges.put("2000-5000 DT", budgetRanges.get("2000-5000 DT") + 1);
                } else {
                    budgetRanges.put("5000+ DT", budgetRanges.get("5000+ DT") + 1);
                }
            }
        }
        
        return budgetRanges.entrySet().stream()
                .map(entry -> new BudgetDistributionDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopClientDTO> getTopClients(int limit) {
        List<JobOffer> allOffers = jobOfferRepository.findAll();
        
        Map<UUID, Long> clientOfferCounts = allOffers.stream()
                .collect(Collectors.groupingBy(JobOffer::getClientId, Collectors.counting()));
        
        return clientOfferCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(entry -> new TopClientDTO(
                        entry.getKey().toString(),
                        "Client " + entry.getKey().toString().substring(0, 8), // Placeholder name
                        entry.getValue(),
                        85 + new Random().nextInt(15) // Random completion rate 85-99%
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyDataDTO> getMonthlyData(int months) {
        List<JobOffer> allOffers = jobOfferRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        
        Map<String, MonthlyDataDTO> monthlyMap = new LinkedHashMap<>();
        
        // Initialize last N months
        for (int i = months - 1; i >= 0; i--) {
            LocalDateTime monthDate = now.minusMonths(i);
            String monthKey = monthDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            monthlyMap.put(monthKey, new MonthlyDataDTO(monthKey, 0, 0));
        }
        
        // Count offers per month
        for (JobOffer offer : allOffers) {
            if (offer.getCreatedAt() != null) {
                LocalDateTime createdAt = offer.getCreatedAt();
                if (createdAt.isAfter(now.minusMonths(months))) {
                    String monthKey = createdAt.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    MonthlyDataDTO monthData = monthlyMap.get(monthKey);
                    if (monthData != null) {
                        monthData.setOffers(monthData.getOffers() + 1);
                        monthData.setApplications(monthData.getApplications() + 
                                (offer.getApplicantCount() != null ? offer.getApplicantCount() : 0));
                    }
                }
            }
        }
        
        return new ArrayList<>(monthlyMap.values());
    }}