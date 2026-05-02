package tn.esprit.pi.nexlance.dto;

import lombok.Data;
import tn.esprit.pi.nexlance.entities.User;

@Data
public class UpdateStatusRequest {
    private User.UserStatus status;
}
