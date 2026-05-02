package tn.esprit.pi.nexlance.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nexlance.dto.AuthResponse;
import tn.esprit.pi.nexlance.dto.LoginRequest;
import tn.esprit.pi.nexlance.dto.RegisterRequest;
import tn.esprit.pi.nexlance.services.AuthService;
import tn.esprit.pi.nexlance.services.SimpleAuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // On garde l'injection optionnelle selon le profil actif
    @Autowired(required = false)
    private AuthService authService;

    @Autowired(required = false)
    private SimpleAuthService simpleAuthService;

    /**
     * Helper pour exécuter les actions sur le service disponible.
     * Si simpleAuthService est présent (keycloak.enabled=false), on l'utilise.
     */
    private boolean isSimpleAuth() {
        return simpleAuthService != null;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = isSimpleAuth() ?
                    simpleAuthService.register(request) : authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = isSimpleAuth() ?
                    simpleAuthService.login(request) : authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Statut 401 pour un échec de login
            return buildErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("refreshToken");
            AuthResponse response = isSimpleAuth() ?
                    simpleAuthService.refreshToken(token) : authService.refreshToken(token);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");
            AuthResponse response = isSimpleAuth() ?
                    simpleAuthService.verifyEmail(email, code) : authService.verifyEmail(email, code);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (isSimpleAuth()) simpleAuthService.requestPasswordReset(email);
            else authService.requestPasswordReset(email);
            return ResponseEntity.ok(Map.of("message", "Password reset email sent"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            if (isSimpleAuth()) simpleAuthService.resetPassword(email, token, newPassword);
            else authService.resetPassword(email, token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Petit utilitaire pour uniformiser les erreurs
    private ResponseEntity<AuthResponse> buildErrorResponse(String message, HttpStatus status) {
        AuthResponse errorResponse = new AuthResponse();
        errorResponse.setMessage(message);
        return ResponseEntity.status(status).body(errorResponse);
    }
}