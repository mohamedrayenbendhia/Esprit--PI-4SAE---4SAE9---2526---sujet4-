package tn.esprit.pi.nexlance.dto;

import lombok.*;
import tn.esprit.pi.nexlance.entities.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String email;
    private String password;
    private String confirmPassword;
    private User.UserType type;
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
