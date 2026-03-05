package tn.esprit.pi.nexlance.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nexlance.dto.AuthResponse;
import tn.esprit.pi.nexlance.dto.LoginRequest;
import tn.esprit.pi.nexlance.dto.RegisterRequest;
import tn.esprit.pi.nexlance.services.SimpleAuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SimpleAuthService simpleAuthService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(simpleAuthService.register(request));
        } catch (RuntimeException e) {
            AuthResponse error = new AuthResponse();
            error.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(simpleAuthService.login(request));
        } catch (RuntimeException e) {
            AuthResponse error = new AuthResponse();
            error.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(simpleAuthService.refreshToken(request.get("refreshToken")));
        } catch (RuntimeException e) {
            AuthResponse error = new AuthResponse();
            error.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(simpleAuthService.verifyEmail(
                    request.get("email"), request.get("code")));
        } catch (RuntimeException e) {
            AuthResponse error = new AuthResponse();
            error.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @RequestBody Map<String, String> request) {
        try {
            simpleAuthService.requestPasswordReset(request.get("email"));
            return ResponseEntity.ok(Map.of("message", "Password reset email sent"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody Map<String, String> request) {
        try {
            simpleAuthService.resetPassword(
                    request.get("email"),
                    request.get("token"),
                    request.get("newPassword"));
            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}