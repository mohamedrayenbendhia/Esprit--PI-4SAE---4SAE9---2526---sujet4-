package tn.esprit.pi.nexlance.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi.nexlance.recommendation.enums.RecommendationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDTO {
    private Long id;
    private String clientId;
    private String freelanceId;
    private String jobOfferId;
    private String message;
    private BigDecimal proposedBudget;
    private LocalDate deadline;
    private RecommendationStatus status;
    private String freelanceResponse;
    private LocalDateTime respondedAt;
    private Integer viewsCount;
    private Integer remindersSent;
    private LocalDateTime lastRemindedAt;
    private LocalDateTime expiresAt;
    private String cancelledReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
