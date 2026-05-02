package tn.esprit.pi.nexlance.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nexlance.entities.User;
import tn.esprit.pi.nexlance.services.UserService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/internal")
@RequiredArgsConstructor
public class UserInternalController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, String>> getUserInternal(@PathVariable UUID id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(Map.of(
                "email",     user.getEmail()     != null ? user.getEmail()     : "",
                "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                "lastName",  user.getLastName()  != null ? user.getLastName()  : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
