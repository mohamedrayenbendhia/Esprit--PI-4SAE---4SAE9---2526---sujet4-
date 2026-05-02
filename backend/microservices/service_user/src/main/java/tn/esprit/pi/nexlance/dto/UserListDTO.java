package tn.esprit.pi.nexlance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi.nexlance.entities.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private User.UserType type;
    private User.UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
