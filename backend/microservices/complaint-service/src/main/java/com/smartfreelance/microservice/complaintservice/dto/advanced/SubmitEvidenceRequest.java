package com.smartfreelance.microservice.complaintservice.dto.advanced;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SubmitEvidenceRequest {
    @NotBlank
    private String description;
    private List<String> attachments;
}
