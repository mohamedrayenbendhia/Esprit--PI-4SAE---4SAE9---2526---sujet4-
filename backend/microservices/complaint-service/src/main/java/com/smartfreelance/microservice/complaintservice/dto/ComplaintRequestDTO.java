package com.smartfreelance.microservice.complaintservice.dto;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ComplaintRequestDTO {

    // reporterId est injecté par le controller via X-User-Id, pas envoyé par le client
    private String reporterId;

    private String reportedUserId;

    private String reportedUserEmail;

    private String projectId;

    @NotNull
    private Complaint.ComplaintCategory category;

    private Complaint.Priority priority;

    @NotBlank
    private String subject;

    @NotBlank
    private String description;

    private List<String> attachments;
}
