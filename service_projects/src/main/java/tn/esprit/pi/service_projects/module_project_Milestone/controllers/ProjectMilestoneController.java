package tn.esprit.pi.service_projects.module_project_Milestone.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.MilestoneStatus;
import tn.esprit.pi.service_projects.module_project_Milestone.entities.ProjectMilestone;
import tn.esprit.pi.service_projects.module_project_Milestone.services.ProjectMilestoneService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/milestones")
@CrossOrigin(origins = "*")
public class ProjectMilestoneController {

    private final ProjectMilestoneService milestoneService;

    @Autowired
    public ProjectMilestoneController(ProjectMilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }

    /**
     * Create a new milestone
     * POST /api/milestones
     */
    @PostMapping
    public ResponseEntity<ProjectMilestone> createMilestone(@RequestBody ProjectMilestone milestone) {
        ProjectMilestone createdMilestone = milestoneService.createMilestone(milestone);
        return new ResponseEntity<>(createdMilestone, HttpStatus.CREATED);
    }

    /**
     * Get all milestones
     * GET /api/milestones
     */
    @GetMapping
    public ResponseEntity<List<ProjectMilestone>> getAllMilestones() {
        List<ProjectMilestone> milestones = milestoneService.getAllMilestones();
        return ResponseEntity.ok(milestones);
    }

    /**
     * Get milestone by ID
     * GET /api/milestones/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectMilestone> getMilestoneById(@PathVariable UUID id) {
        return milestoneService.getMilestoneById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get milestones by project ID
     * GET /api/milestones/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProjectMilestone>> getMilestonesByProjectId(@PathVariable UUID projectId) {
        List<ProjectMilestone> milestones = milestoneService.getMilestonesByProjectId(projectId);
        return ResponseEntity.ok(milestones);
    }

    /**
     * Get milestones by project ID and status
     * GET /api/milestones/project/{projectId}/status/{status}
     */
    @GetMapping("/project/{projectId}/status/{status}")
    public ResponseEntity<List<ProjectMilestone>> getMilestonesByProjectIdAndStatus(
            @PathVariable UUID projectId,
            @PathVariable MilestoneStatus status) {
        List<ProjectMilestone> milestones = milestoneService.getMilestonesByProjectIdAndStatus(projectId, status);
        return ResponseEntity.ok(milestones);
    }

    /**
     * Get milestones by status
     * GET /api/milestones/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProjectMilestone>> getMilestonesByStatus(@PathVariable MilestoneStatus status) {
        List<ProjectMilestone> milestones = milestoneService.getMilestonesByStatus(status);
        return ResponseEntity.ok(milestones);
    }

    /**
     * Get overdue milestones
     * GET /api/milestones/overdue
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<ProjectMilestone>> getOverdueMilestones() {
        List<ProjectMilestone> milestones = milestoneService.getOverdueMilestones();
        return ResponseEntity.ok(milestones);
    }

    /**
     * Get milestones due soon (within specified days)
     * GET /api/milestones/due-soon?days=7
     */
    @GetMapping("/due-soon")
    public ResponseEntity<List<ProjectMilestone>> getMilestonesDueSoon(@RequestParam(defaultValue = "7") int days) {
        List<ProjectMilestone> milestones = milestoneService.getMilestonesDueSoon(days);
        return ResponseEntity.ok(milestones);
    }

    /**
     * Update a milestone
     * PUT /api/milestones/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectMilestone> updateMilestone(
            @PathVariable UUID id,
            @RequestBody ProjectMilestone milestone) {
        try {
            ProjectMilestone updatedMilestone = milestoneService.updateMilestone(id, milestone);
            return ResponseEntity.ok(updatedMilestone);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Submit a milestone (by freelance)
     * POST /api/milestones/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<ProjectMilestone> submitMilestone(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payload) {
        try {
            String attachments = payload.get("attachments");
            String comment = payload.get("comment");
            ProjectMilestone submittedMilestone = milestoneService.submitMilestone(id, attachments, comment);
            return ResponseEntity.ok(submittedMilestone);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Approve a milestone (by client)
     * POST /api/milestones/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ProjectMilestone> approveMilestone(@PathVariable UUID id) {
        try {
            ProjectMilestone approvedMilestone = milestoneService.approveMilestone(id);
            return ResponseEntity.ok(approvedMilestone);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reject a milestone (by client)
     * POST /api/milestones/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ProjectMilestone> rejectMilestone(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payload) {
        try {
            String rejectionReason = payload.get("rejectionReason");
            ProjectMilestone rejectedMilestone = milestoneService.rejectMilestone(id, rejectionReason);
            return ResponseEntity.ok(rejectedMilestone);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update milestone status
     * PATCH /api/milestones/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProjectMilestone> updateMilestoneStatus(
            @PathVariable UUID id,
            @RequestParam MilestoneStatus status) {
        try {
            ProjectMilestone updatedMilestone = milestoneService.updateMilestoneStatus(id, status);
            return ResponseEntity.ok(updatedMilestone);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a milestone
     * DELETE /api/milestones/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMilestone(@PathVariable UUID id) {
        milestoneService.deleteMilestone(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin approve milestone (force approval)
     * POST /api/milestones/{id}/admin-approve
     */
    @PostMapping("/{id}/admin-approve")
    public ResponseEntity<ProjectMilestone> adminApproveMilestone(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payload) {
        try {
            String adminNotes = payload.get("adminNotes");
            ProjectMilestone approvedMilestone = milestoneService.adminApproveMilestone(id, adminNotes);
            return ResponseEntity.ok(approvedMilestone);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Admin request revisions
     * POST /api/milestones/{id}/admin-revisions
     */
    @PostMapping("/{id}/admin-revisions")
    public ResponseEntity<ProjectMilestone> adminRequestRevisions(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payload) {
        try {
            String adminNotes = payload.get("adminNotes");
            ProjectMilestone milestone = milestoneService.adminRequestRevisions(id, adminNotes);
            return ResponseEntity.ok(milestone);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get milestone statistics
     * GET /api/milestones/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMilestoneStats() {
        List<ProjectMilestone> allMilestones = milestoneService.getAllMilestones();

        long total = allMilestones.size();
        long pending = allMilestones.stream().filter(m -> m.getStatus() == MilestoneStatus.PENDING).count();
        long inProgress = allMilestones.stream().filter(m -> m.getStatus() == MilestoneStatus.IN_PROGRESS).count();
        long submitted = allMilestones.stream().filter(m -> m.getStatus() == MilestoneStatus.SUBMITTED).count();
        long approved = allMilestones.stream().filter(m -> m.getStatus() == MilestoneStatus.APPROVED).count();
        long rejected = allMilestones.stream().filter(m -> m.getStatus() == MilestoneStatus.REJECTED).count();
        long overdue = allMilestones.stream()
                .filter(m -> m.getDueDate() != null
                        && m.getDueDate().isBefore(java.time.LocalDateTime.now())
                        && m.getStatus() != MilestoneStatus.APPROVED)
                .count();

        double completionRate = total > 0 ? (double) approved / total * 100 : 0;

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("inProgress", inProgress);
        stats.put("submitted", submitted);
        stats.put("approved", approved);
        stats.put("rejected", rejected);
        stats.put("overdue", overdue);
        stats.put("completionRate", Math.round(completionRate * 10.0) / 10.0);

        return ResponseEntity.ok(stats);
    }
}
