package tn.esprit.pi.nexlance.invitation.dto;

import lombok.Data;
import tn.esprit.pi.nexlance.invitation.enums.InvitationStatus;

@Data
public class UpdateInvitationDto {
    private InvitationStatus status;
    private String message;
}
