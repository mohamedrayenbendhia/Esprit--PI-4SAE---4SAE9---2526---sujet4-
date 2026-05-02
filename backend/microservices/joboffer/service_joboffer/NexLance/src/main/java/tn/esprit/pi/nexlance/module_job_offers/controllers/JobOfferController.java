package tn.esprit.pi.nexlance.module_job_offers.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.pi.nexlance.module_job_offers.dto.*;
import tn.esprit.pi.nexlance.module_job_offers.entities.JobOffer;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobCategory;
import tn.esprit.pi.nexlance.module_job_offers.enums.JobOfferStatus;
import tn.esprit.pi.nexlance.module_job_offers.enums.ExperienceLevel;
import tn.esprit.pi.nexlance.module_job_offers.services.JobOfferService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/job-offers")
public class JobOfferController {

    private final JobOfferService jobOfferService;

    @Value("${file.upload-dir:./uploads/attachments}")
    private String uploadDir;

    @Autowired
    public JobOfferController(JobOfferService jobOfferService) {
        this.jobOfferService = jobOfferService;
    }

    @PostMapping
    public ResponseEntity<JobOffer> createJobOffer(@RequestBody JobOffer jobOffer) {
        try {
            JobOffer createdJobOffer = jobOfferService.createJobOffer(jobOffer);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdJobOffer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobOffer> getJobOfferById(@PathVariable UUID id) {
        try {
            JobOffer jobOffer = jobOfferService.getJobOfferById(id);
            jobOfferService.incrementViewCount(id);
            return ResponseEntity.ok(jobOffer);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<JobOffer>> getAllJobOffers() {
        List<JobOffer> jobOffers = jobOfferService.getAllJobOffers();
        return ResponseEntity.ok(jobOffers);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<JobOffer>> getJobOffersByStatus(@PathVariable JobOfferStatus status) {
        List<JobOffer> jobOffers = jobOfferService.getJobOffersByStatus(status);
        return ResponseEntity.ok(jobOffers);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<JobOffer>> getJobOffersByClientId(@PathVariable UUID clientId) {
        List<JobOffer> jobOffers = jobOfferService.getJobOffersByClientId(clientId);
        return ResponseEntity.ok(jobOffers);
    }

    @GetMapping("/active")
    public ResponseEntity<List<JobOffer>> getActiveJobOffers() {
        List<JobOffer> jobOffers = jobOfferService.getActiveJobOffers();
        return ResponseEntity.ok(jobOffers);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<JobOffer>> getJobOffersByCategory(@PathVariable JobCategory category) {
        List<JobOffer> jobOffers = jobOfferService.getJobOffersByCategory(category);
        return ResponseEntity.ok(jobOffers);
    }

    @GetMapping("/remote")
    public ResponseEntity<List<JobOffer>> getRemoteJobOffers() {
        List<JobOffer> jobOffers = jobOfferService.getRemoteJobOffers();
        return ResponseEntity.ok(jobOffers);
    }

    @GetMapping("/experience-level/{level}")
    public ResponseEntity<List<JobOffer>> getJobOffersByExperienceLevel(@PathVariable ExperienceLevel level) {
        List<JobOffer> jobOffers = jobOfferService.getJobOffersByExperienceLevel(level);
        return ResponseEntity.ok(jobOffers);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobOffer> updateJobOffer(@PathVariable UUID id, @RequestBody JobOffer jobOffer) {
        try {
            JobOffer updatedJobOffer = jobOfferService.updateJobOffer(id, jobOffer);
            return ResponseEntity.ok(updatedJobOffer);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<JobOffer> updateJobOfferStatus(
            @PathVariable UUID id,
            @RequestParam JobOfferStatus status) {
        try {
            JobOffer updatedJobOffer = jobOfferService.updateJobOfferStatus(id, status);
            return ResponseEntity.ok(updatedJobOffer);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobOffer(@PathVariable UUID id) {
        try {
            jobOfferService.deleteJobOffer(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<JobOffer> archiveJobOffer(@PathVariable UUID id) {
        try {
            JobOffer archivedJobOffer = jobOfferService.archiveJobOffer(id);
            return ResponseEntity.ok(archivedJobOffer);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get job offer statistics
     * GET /api/job-offers/stats
     * @return Statistics about job offers
     */
    @GetMapping("/stats")
    public ResponseEntity<JobOfferStatsDTO> getJobOfferStats() {
        JobOfferStatsDTO stats = jobOfferService.getJobOfferStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get category distribution
     * GET /api/job-offers/analytics/categories
     * @return Category distribution data
     */
    @GetMapping("/analytics/categories")
    public ResponseEntity<List<CategoryDistributionDTO>> getCategoryDistribution() {
        List<CategoryDistributionDTO> data = jobOfferService.getCategoryDistribution();
        return ResponseEntity.ok(data);
    }

    /**
     * Get budget distribution
     * GET /api/job-offers/analytics/budget
     * @return Budget distribution data
     */
    @GetMapping("/analytics/budget")
    public ResponseEntity<List<BudgetDistributionDTO>> getBudgetDistribution() {
        List<BudgetDistributionDTO> data = jobOfferService.getBudgetDistribution();
        return ResponseEntity.ok(data);
    }

    /**
     * Get top clients
     * GET /api/job-offers/analytics/top-clients
     * @param limit Number of top clients to return (default: 5)
     * @return Top clients data
     */
    @GetMapping("/analytics/top-clients")
    public ResponseEntity<List<TopClientDTO>> getTopClients(@RequestParam(defaultValue = "5") int limit) {
        List<TopClientDTO> data = jobOfferService.getTopClients(limit);
        return ResponseEntity.ok(data);
    }

    /**
     * Get monthly data
     * GET /api/job-offers/analytics/monthly
     * @param months Number of months to include (default: 6)
     * @return Monthly statistics data
     */
    @GetMapping("/analytics/monthly")
    public ResponseEntity<List<MonthlyDataDTO>> getMonthlyData(@RequestParam(defaultValue = "6") int months) {
        List<MonthlyDataDTO> data = jobOfferService.getMonthlyData(months);
        return ResponseEntity.ok(data);
    }

    /**
     * Upload attachments for job offers
     * POST /api/job-offers/upload
     * @param files List of files to upload
     * @return List of file URLs/paths
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadAttachments(@RequestParam("files") MultipartFile[] files) {
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            List<String> fileUrls = new ArrayList<>();

            for (MultipartFile file : files) {
                // Generate unique filename
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String uniqueFilename = UUID.randomUUID().toString() + extension;

                // Save file
                Path filePath = uploadPath.resolve(uniqueFilename);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Add URL to response (you can customize this based on your serving strategy)
                String fileUrl = "/uploads/attachments/" + uniqueFilename;
                fileUrls.add(fileUrl);
            }

            return ResponseEntity.ok(fileUrls);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload files: " + e.getMessage());
        }
    }
}
