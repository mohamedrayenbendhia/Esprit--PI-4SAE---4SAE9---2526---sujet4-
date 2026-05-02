package com.smartfreelance.microservice.complaintservice.dto;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import lombok.Data;

import java.util.List;

@Data
public class ComplaintUpdateDTO {

    private String subject;
    private String description;
    private Complaint.ComplaintCategory category;
    private Complaint.Priority priority;
    private Complaint.Status status;
    private String assignedToId;
    private String resolution;
    private Complaint.ResolutionType resolutionType;
    private Integer satisfactionRating;
    private List<String> attachments;
}
