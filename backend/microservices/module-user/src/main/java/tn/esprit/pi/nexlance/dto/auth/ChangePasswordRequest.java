package tn.esprit.pi.nexlance.dto.auth;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la requête de changement de mot de passe
 */
@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Le mot de passe actuel est requis")
    private String currentPassword;

    @NotBlank(message = "Le nouveau mot de passe est requis")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String newPassword;

    @NotBlank(message = "La confirmation est requise")
    private String confirmPassword;
}
