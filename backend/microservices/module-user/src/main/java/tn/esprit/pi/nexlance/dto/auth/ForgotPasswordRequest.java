package tn.esprit.pi.nexlance.dto.auth;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour la demande de réinitialisation de mot de passe
 */
@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;
}
