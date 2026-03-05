package tn.esprit.pi.nexlance.controllers;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.pi.nexlance.dto.auth.ChangePasswordRequest;
import tn.esprit.pi.nexlance.entities.User;
import tn.esprit.pi.nexlance.services.UserService;
import tn.esprit.pi.nexlance.services.FileUploadService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final FileUploadService fileUploadService;

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            User user = userService.getUserById(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateCurrentUser(
            @RequestBody User userData,
            Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            User updatedUser = userService.updateUser(userId, userData);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            String avatarUrl = fileUploadService.uploadAvatar(file, userId);
            User user = userService.getUserById(userId);
            user.setAvatar(avatarUrl);
            User updatedUser = userService.updateUser(userId, user);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload avatar: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changeMyPassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            userService.changeMyPassword(userId, request);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser(Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            userService.deleteUser(userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<Page<User>> getAllUsers(Pageable pageable) {
        Page<User> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<Page<User>> getUsersByType(@PathVariable User.UserType type, Pageable pageable) {
        Page<User> users = userService.getUsersByType(type, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<User>> getUsersByStatus(@PathVariable User.UserStatus status, Pageable pageable) {
        Page<User> users = userService.getUsersByStatus(status, pageable);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable UUID id, @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<User> updatePassword(@PathVariable UUID id, @RequestBody String newPassword) {
        User updatedUser = userService.updatePassword(id, newPassword);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<User> updateStatus(@PathVariable UUID id, @RequestBody User.UserStatus status) {
        User updatedUser = userService.updateStatus(id, status);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}/verify-email")
    public ResponseEntity<User> verifyEmail(@PathVariable UUID id) {
        User updatedUser = userService.verifyEmail(id);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}/last-login")
    public ResponseEntity<User> updateLastLogin(@PathVariable UUID id) {
        User updatedUser = userService.updateLastLogin(id);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null) {
            Object details = authentication.getDetails();
            if (details instanceof Claims claims) {
                String userId = claims.get("userId", String.class);
                if (userId != null) return UUID.fromString(userId);
            }
            try {
                return UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException e) {
                return userService.getUserByEmail(authentication.getName()).getId();
            }
        }
        throw new RuntimeException("Unable to extract user ID from authentication");
    }
}