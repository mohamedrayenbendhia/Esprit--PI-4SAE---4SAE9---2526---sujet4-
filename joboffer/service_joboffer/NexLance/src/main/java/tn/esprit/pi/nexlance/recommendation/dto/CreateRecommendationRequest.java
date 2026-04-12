package tn.esprit.pi.nexlance.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecommendationRequest {
    private String clientId;
    private String freelanceId;
    private String jobOfferId;
    private String message;
    private BigDecimal proposedBudget;
    private LocalDate deadline;
    private LocalDateTime expiresAt;
}
