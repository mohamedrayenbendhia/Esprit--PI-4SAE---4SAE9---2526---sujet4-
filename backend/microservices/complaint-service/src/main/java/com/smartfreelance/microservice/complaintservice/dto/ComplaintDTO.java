package com.smartfreelance.microservice.complaintservice.dto;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintDTO {

    private String id;
    private String ticketNumber;
    private String reporterId;
    private String reportedUserId;
    private String projectId;
    private Complaint.ComplaintCategory category;
    private Complaint.Priority priority;
    private Complaint.Status status;
    private String subject;
    private String description;
    private List<String> attachments;
    private String assignedToId;
    private String resolution;
    private Complaint.ResolutionType resolutionType;
    private Integer satisfactionRating;
    private LocalDateTime createdAt;
    private LocalDateTime firstResponseAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime updatedAt;
}