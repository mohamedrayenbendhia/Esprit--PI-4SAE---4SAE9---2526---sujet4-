package tn.esprit.pi.nexlance.dto.auth;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la requête d'inscription
 */
@Data
public class RegisterRequest {
    @NotBlank(message = "Le prénom est requis")
    private String firstName;

    @NotBlank(message = "Le nom est requis")
    private String lastName;

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est requis")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    private String phoneNumber;

    @NotBlank(message = "Le type d'utilisateur est requis")
    private String type; // CLIENT, FREELANCE, ADMIN
}
