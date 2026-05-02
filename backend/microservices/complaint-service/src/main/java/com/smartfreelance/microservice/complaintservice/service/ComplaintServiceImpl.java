package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.Complaint.Priority;
import com.smartfreelance.microservice.complaintservice.entity.Complaint.Status;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final SlaService slaService;

    @Override
    public Complaint createComplaint(Complaint complaint) {
        log.info("Creating new complaint with subject: {}", complaint.getSubject());

        if (complaint.getReporterId() == null || complaint.getReporterId().isEmpty()) {
            throw new IllegalArgumentException("Reporter ID cannot be null or empty");
        }

        if (complaint.getSubject() == null || complaint.getSubject().isEmpty()) {
            throw new IllegalArgumentException("Subject cannot be null or empty");
        }

        if (complaint.getPriority() == null) {
            complaint.setPriority(Priority.MEDIUM);
        }

        if (complaint.getStatus() == null) {
            complaint.setStatus(Status.OPEN);
        }

        // Générer un ticket number unique avec retry (ThreadLocalRandom thread-safe)
        complaint.setTicketNumber(generateUniqueTicketNumber());

        Complaint savedComplaint = complaintRepository.save(complaint);
        log.info("Complaint created successfully with ticket number: {}", savedComplaint.getTicketNumber());

        // Initialiser le tracking SLA automatiquement à la création
        slaService.initTracking(savedComplaint.getId(), savedComplaint.getPriority());

        return savedComplaint;
    }

    @Override
    @Transactional(readOnly = true)
    public Complaint getComplaintById(String id) {
        log.debug("Fetching complaint with id: {}", id);
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Complaint getComplaintByTicketNumber(String ticketNumber) {
        log.debug("Fetching complaint with ticket number: {}", ticketNumber);
        return complaintRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ticket number: " + ticketNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getAllComplaints() {
        log.debug("Fetching all complaints");
        return complaintRepository.findAll();
    }

    @Override
    public Complaint updateComplaint(String id, Complaint complaintDetails) {
        log.info("Updating complaint with id: {}", id);

        Complaint existingComplaint = getComplaintById(id);

        if (complaintDetails.getSubject() != null) {
            existingComplaint.setSubject(complaintDetails.getSubject());
        }
        if (complaintDetails.getDescription() != null) {
            existingComplaint.setDescription(complaintDetails.getDescription());
        }
        if (complaintDetails.getCategory() != null) {
            existingComplaint.setCategory(complaintDetails.getCategory());
        }
        if (complaintDetails.getPriority() != null) {
            existingComplaint.setPriority(complaintDetails.getPriority());
        }
        if (complaintDetails.getAttachments() != null) {
            existingComplaint.setAttachments(complaintDetails.getAttachments());
        }

        Complaint updatedComplaint = complaintRepository.save(existingComplaint);
        log.info("Complaint updated successfully: {}", updatedComplaint.getTicketNumber());

        return updatedComplaint;
    }

    @Override
    public void deleteComplaint(String id) {
        log.info("Deleting complaint with id: {}", id);
        Complaint complaint = getComplaintById(id);
        complaintRepository.delete(complaint);
        log.info("Complaint deleted successfully: {}", complaint.getTicketNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getComplaintsByReporter(String reporterId) {
        log.debug("Fetching complaints for reporter: {}", reporterId);
        return complaintRepository.findByReporterId(reporterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getComplaintsByReportedUser(String reportedUserId) {
        log.debug("Fetching complaints for reported user: {}", reportedUserId);
        return complaintRepository.findByReportedUserId(reportedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getComplaintsByProject(String projectId) {
        log.debug("Fetching complaints for project: {}", projectId);
        return complaintRepository.findByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getComplaintsByStatus(Status status) {
        log.debug("Fetching complaints with status: {}", status);
        return complaintRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getComplaintsByPriority(Priority priority) {
        log.debug("Fetching complaints with priority: {}", priority);
        return complaintRepository.findByPriority(priority);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getComplaintsByAssignedAgent(String assignedToId) {
        log.debug("Fetching complaints assigned to: {}", assignedToId);
        return complaintRepository.findByAssignedToId(assignedToId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getUnassignedComplaints(Status status) {
        log.debug("Fetching unassigned complaints with status: {}", status);
        return complaintRepository.findUnassignedByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getComplaintsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching complaints between {} and {}", startDate, endDate);
        return complaintRepository.findByDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getOverdueComplaints(int daysThreshold) {
        log.debug("Fetching overdue complaints (older than {} days)", daysThreshold);
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(daysThreshold);
        return complaintRepository.findOverdueComplaints(thresholdDate);
    }

    @Override
    public Complaint assignComplaint(String complaintId, String agentId) {
        log.info("Assigning complaint {} to agent {}", complaintId, agentId);

        Complaint complaint = getComplaintById(complaintId);
        complaint.setAssignedToId(agentId);

        if (complaint.getStatus() == Status.OPEN) {
            complaint.setStatus(Status.IN_PROGRESS);
        }

        Complaint updatedComplaint = complaintRepository.save(complaint);
        log.info("Complaint assigned successfully");

        return updatedComplaint;
    }

    @Override
    public Complaint updateStatus(String complaintId, Status newStatus) {
        log.info("Updating status of complaint {} to {}", complaintId, newStatus);

        Complaint complaint = getComplaintById(complaintId);
        complaint.setStatus(newStatus);

        if (newStatus == Status.RESOLVED && complaint.getResolvedAt() == null) {
            complaint.setResolvedAt(LocalDateTime.now());
        } else if (newStatus == Status.CLOSED && complaint.getClosedAt() == null) {
            complaint.setClosedAt(LocalDateTime.now());
        }

        return complaintRepository.save(complaint);
    }

    @Override
    public Complaint updatePriority(String complaintId, Priority newPriority) {
        log.info("Updating priority of complaint {} to {}", complaintId, newPriority);

        Complaint complaint = getComplaintById(complaintId);
        complaint.setPriority(newPriority);

        return complaintRepository.save(complaint);
    }

    @Override
    public Complaint resolveComplaint(String complaintId, String resolution, Complaint.ResolutionType resolutionType) {
        log.info("Resolving complaint {} with resolution type: {}", complaintId, resolutionType);

        Complaint complaint = getComplaintById(complaintId);
        complaint.setResolution(resolution);
        complaint.setResolutionType(resolutionType);
        complaint.setStatus(Status.RESOLVED);
        complaint.setResolvedAt(LocalDateTime.now());

        return complaintRepository.save(complaint);
    }

    @Override
    public Complaint closeComplaint(String complaintId) {
        log.info("Closing complaint {}", complaintId);

        Complaint complaint = getComplaintById(complaintId);

        if (complaint.getStatus() != Status.RESOLVED) {
            throw new IllegalStateException("Cannot close complaint that is not resolved");
        }

        complaint.setStatus(Status.CLOSED);
        complaint.setClosedAt(LocalDateTime.now());

        return complaintRepository.save(complaint);
    }

    @Override
    public Complaint rateComplaint(String complaintId, Integer rating) {
        log.info("Rating complaint {} with rating: {}", complaintId, rating);

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Complaint complaint = getComplaintById(complaintId);

        if (complaint.getStatus() != Status.CLOSED) {
            throw new IllegalStateException("Can only rate closed complaints");
        }

        complaint.setSatisfactionRating(rating);

        return complaintRepository.save(complaint);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(Status status) {
        return complaintRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByPriority(Priority priority) {
        return complaintRepository.countByPriority(priority);
    }

    @Override
    public Complaint escalateComplaint(String complaintId) {
        log.info("Escalating complaint {}", complaintId);
        Complaint complaint = getComplaintById(complaintId);
        complaint.setStatus(Status.ESCALATED);
        return complaintRepository.save(complaint);
    }

    @Override
    public Complaint addAttachment(String complaintId, String attachmentUrl) {
        log.info("Adding attachment to complaint {}", complaintId);
        Complaint complaint = getComplaintById(complaintId);
        if (complaint.getAttachments() == null) {
            complaint.setAttachments(new java.util.ArrayList<>());
        }
        complaint.getAttachments().add(attachmentUrl);
        return complaintRepository.save(complaint);
    }

    @Override
    public Complaint rateSatisfaction(String complaintId, int rating) {
        return rateComplaint(complaintId, rating);
    }

    // ── Génération ticket number avec retry anti-collision ────────────────────

    private String generateUniqueTicketNumber() {
        LocalDateTime now = LocalDateTime.now();
        String yearMonth  = now.getYear() + String.format("%02d", now.getMonthValue());

        for (int attempt = 0; attempt < 5; attempt++) {
            int rand = ThreadLocalRandom.current().nextInt(100000, 999999); // 6 chiffres
            String candidate = "NX-" + yearMonth + "-" + rand;
            if (!complaintRepository.existsByTicketNumber(candidate)) {
                return candidate;
            }
            log.warn("[TicketNumber] Collision sur {} — tentative {}/5", candidate, attempt + 1);
        }

        // Fallback UUID court (8 chars hex) si 5 collisions consécutives (très improbable)
        String fallback = "NX-" + yearMonth + "-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
        log.warn("[TicketNumber] Fallback UUID utilisé : {}", fallback);
        return fallback;
    }

    @Override
    public Complaint involveReportedUser(String complaintId, String reportedUserId) {
        log.info("Involving reported user {} in complaint {}", reportedUserId, complaintId);
        Complaint complaint = getComplaintById(complaintId);
        complaint.setReportedUserId(reportedUserId);
        return complaintRepository.save(complaint);
    }

}