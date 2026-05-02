package com.smartfreelance.microservice.complaintservice.dto.advanced;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseTemplateResponse {
    private String id;
    private String title;
    private String content;
    private Complaint.ComplaintCategory category;
    private String createdByAdminId;
    private int usageCount;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
