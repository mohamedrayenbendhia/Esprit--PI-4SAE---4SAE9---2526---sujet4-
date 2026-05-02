package tn.esprit.pi.nexlance.module_job_offers.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nexlance.module_job_offers.entities.Application;
import tn.esprit.pi.nexlance.module_job_offers.enums.ApplicationStatus;
import tn.esprit.pi.nexlance.module_job_offers.services.ApplicationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "*")
public class ApplicationController {

    private final ApplicationService applicationService;

    @Autowired
    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<Application> createApplication(@RequestBody Application application) {
        try {
            Application createdApplication = applicationService.createApplication(application);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdApplication);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Application> getApplicationById(@PathVariable UUID id) {
        try {
            Application application = applicationService.getApplicationById(id);
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Application>> getAllApplications() {
        List<Application> applications = applicationService.getAllApplications();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/job-offer/{jobOfferId}")
    public ResponseEntity<List<Application>> getApplicationsByJobOfferId(@PathVariable UUID jobOfferId) {
        List<Application> applications = applicationService.getApplicationsByJobOfferId(jobOfferId);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/job-offer/{jobOfferId}/unread")
    public ResponseEntity<List<Application>> getUnreadApplicationsByJobOfferId(@PathVariable UUID jobOfferId) {
        List<Application> applications = applicationService.getUnreadApplicationsByJobOfferId(jobOfferId);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/job-offer/{jobOfferId}/count")
    public ResponseEntity<Long> countApplicationsByJobOfferId(@PathVariable UUID jobOfferId) {
        Long count = applicationService.countApplicationsByJobOfferId(jobOfferId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/job-offer/{jobOfferId}/count/{status}")
    public ResponseEntity<Long> countApplicationsByJobOfferIdAndStatus(
            @PathVariable UUID jobOfferId,
            @PathVariable ApplicationStatus status) {
        Long count = applicationService.countApplicationsByJobOfferIdAndStatus(jobOfferId, status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/job-offer/{jobOfferId}/counts")
    public ResponseEntity<java.util.Map<String, Long>> getApplicationCountsByStatus(@PathVariable UUID jobOfferId) {
        java.util.Map<String, Long> counts = applicationService.getApplicationCountsByStatus(jobOfferId);
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/freelance/{freelanceId}")
    public ResponseEntity<List<Application>> getApplicationsByFreelanceId(@PathVariable UUID freelanceId) {
        List<Application> applications = applicationService.getApplicationsByFreelanceId(freelanceId);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Application>> getApplicationsByStatus(@PathVariable ApplicationStatus status) {
        List<Application> applications = applicationService.getApplicationsByStatus(status);
        return ResponseEntity.ok(applications);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Application> updateApplication(@PathVariable UUID id, @RequestBody Application application) {
        try {
            Application updatedApplication = applicationService.updateApplication(id, application);
            return ResponseEntity.ok(updatedApplication);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Application> updateApplicationStatus(
            @PathVariable UUID id,
            @RequestParam ApplicationStatus status) {
        try {
            Application updatedApplication = applicationService.updateApplicationStatus(id, status);
            return ResponseEntity.ok(updatedApplication);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Application> markApplicationAsRead(@PathVariable UUID id) {
        try {
            Application application = applicationService.markAsRead(id);
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID id) {
        try {
            applicationService.deleteApplication(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<Application> withdrawApplication(@PathVariable UUID id) {
        try {
            Application application = applicationService.withdrawApplication(id);
            return ResponseEntity.ok(application);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
