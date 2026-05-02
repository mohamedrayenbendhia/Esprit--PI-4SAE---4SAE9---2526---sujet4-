package com.smartfreelance.microservice.organizationservice.dto.response;

import com.smartfreelance.microservice.organizationservice.enums.RfqStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RfqResponse {
    private String id;
    private String organizationId;
    private String requesterId;
    private String title;
    private String description;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private LocalDate deadline;
    private List<String> skillsNeeded;
    private RfqStatus status;
    private String responseMessage;
    private String respondedById;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}
