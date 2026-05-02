package tn.esprit.pi.nexlance.dto.user;

import lombok.Data;

/**
 * DTO pour la mise à jour du profil utilisateur
 */
@Data
public class UserUpdateRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
