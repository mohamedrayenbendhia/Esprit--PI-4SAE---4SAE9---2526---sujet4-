package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.Complaint.Priority;
import com.smartfreelance.microservice.complaintservice.entity.Complaint.Status;

import java.time.LocalDateTime;
import java.util.List;

public interface ComplaintService {

    Complaint createComplaint(Complaint complaint);
    Complaint getComplaintById(String id);
    Complaint getComplaintByTicketNumber(String ticketNumber);
    List<Complaint> getAllComplaints();
    Complaint updateComplaint(String id, Complaint complaint);
    void deleteComplaint(String id);

    List<Complaint> getComplaintsByReporter(String reporterId);
    List<Complaint> getComplaintsByReportedUser(String reportedUserId);
    List<Complaint> getComplaintsByProject(String projectId);
    List<Complaint> getComplaintsByStatus(Status status);
    List<Complaint> getComplaintsByPriority(Priority priority);
    List<Complaint> getComplaintsByAssignedAgent(String assignedToId);
    List<Complaint> getUnassignedComplaints(Status status);
    List<Complaint> getComplaintsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    List<Complaint> getOverdueComplaints(int daysThreshold);

    Complaint assignComplaint(String id, String agentId);
    Complaint updateStatus(String id, Status newStatus);
    Complaint updatePriority(String id, Priority newPriority);
    Complaint resolveComplaint(String id, String resolution, Complaint.ResolutionType resolutionType);
    Complaint closeComplaint(String id);
    Complaint escalateComplaint(String id);
    Complaint addAttachment(String id, String attachmentUrl);
    Complaint rateSatisfaction(String id, int rating);
    Complaint rateComplaint(String id, Integer rating);
    Complaint involveReportedUser(String id, String reportedUserId);

    long countByStatus(Status status);
    long countByPriority(Priority priority);
}
