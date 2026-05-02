package com.smartfreelance.microservice.complaintservice.dto.advanced;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTemplateRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    @NotNull
    private Complaint.ComplaintCategory category;
}
