package tn.esprit.pi.service_projects.module_project_Milestone.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.DeliverableVersion;
import tn.esprit.pi.service_projects.module_project_Milestone.services.DeliverableVersionService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/deliverables")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DeliverableVersionController {

    private final DeliverableVersionService versionService;

    /**
     * Upload new deliverable version for a milestone
     */
    @PostMapping("/milestone/{milestoneId}/upload")
    public ResponseEntity<DeliverableVersion> uploadVersion(
            @PathVariable UUID milestoneId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") UUID uploadedBy,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "changeNotes", required = false) String changeNotes) throws IOException {

        DeliverableVersion version = versionService.uploadNewVersion(
                milestoneId, uploadedBy, description, changeNotes, file);
        return ResponseEntity.ok(version);
    }

    /**
     * Get version history for a milestone
     */
    @GetMapping("/milestone/{milestoneId}")
    public ResponseEntity<List<DeliverableVersion>> getVersionHistory(@PathVariable UUID milestoneId) {
        return ResponseEntity.ok(versionService.getVersionHistory(milestoneId));
    }

    /**
     * Get latest version for a milestone
     */
    @GetMapping("/milestone/{milestoneId}/latest")
    public ResponseEntity<DeliverableVersion> getLatestVersion(@PathVariable UUID milestoneId) {
        DeliverableVersion latest = versionService.getLatestVersion(milestoneId);
        if (latest == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(latest);
    }

    /**
     * Get version count for a milestone
     */
    @GetMapping("/milestone/{milestoneId}/count")
    public ResponseEntity<Long> getVersionCount(@PathVariable UUID milestoneId) {
        return ResponseEntity.ok(versionService.getVersionCount(milestoneId));
    }

    /**
     * Review a deliverable version (approve/reject)
     */
    @PutMapping("/{versionId}/review")
    public ResponseEntity<DeliverableVersion> reviewVersion(
            @PathVariable UUID versionId,
            @RequestBody Map<String, String> body) {

        UUID reviewedBy = UUID.fromString(body.get("reviewedBy"));
        DeliverableVersion.VersionStatus status = DeliverableVersion.VersionStatus.valueOf(body.get("status"));
        String reviewComment = body.get("reviewComment");

        DeliverableVersion reviewed = versionService.reviewVersion(versionId, reviewedBy, status, reviewComment);
        return ResponseEntity.ok(reviewed);
    }
}
