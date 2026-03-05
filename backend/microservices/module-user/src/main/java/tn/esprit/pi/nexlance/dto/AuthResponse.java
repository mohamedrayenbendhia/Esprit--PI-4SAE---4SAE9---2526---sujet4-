package tn.esprit.pi.nexlance.dto;

import lombok.*;
import tn.esprit.pi.nexlance.entities.User;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private User.UserType type;
    private String role; // Ajouté pour la compatibilité frontend (FREELANCER, CLIENT, ADMIN)
    private User.UserStatus status;
    private Boolean emailVerified;
    private String token;
    private String refreshToken;
    private String message;
}
