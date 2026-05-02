package tn.esprit.pi.nexlance.invitation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateInvitationDto {
    private String clientId;
    private String freelanceId;
    private String jobOfferId;
    private String message;
    private BigDecimal proposedBudget;
    private LocalDateTime deadlineResponse;
}
