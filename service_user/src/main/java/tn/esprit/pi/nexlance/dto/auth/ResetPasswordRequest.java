package tn.esprit.pi.nexlance.dto.auth;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la réinitialisation de mot de passe avec token
 */
@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Le token est requis")
    private String token;

    @NotBlank(message = "Le nouveau mot de passe est requis")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String newPassword;
}
