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

    @Autowired(required = false)
    private AuthService authService;
    
    @Autowired(required = false)
    private SimpleAuthService simpleAuthService;

    private Object getAuthService() {
        return simpleAuthService != null ? simpleAuthService : authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response;
            if (simpleAuthService != null) {
                response = simpleAuthService.register(request);
            } else {
                response = authService.register(request);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            AuthResponse errorResponse = new AuthResponse();
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response;
            if (simpleAuthService != null) {
                response = simpleAuthService.login(request);
            } else {
                response = authService.login(request);
            }
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            AuthResponse errorResponse = new AuthResponse();
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            AuthResponse response;
            if (simpleAuthService != null) {
                response = simpleAuthService.refreshToken(refreshToken);
            } else {
                response = authService.refreshToken(refreshToken);
            }
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            AuthResponse errorResponse = new AuthResponse();
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");
            AuthResponse response;
            if (simpleAuthService != null) {
                response = simpleAuthService.verifyEmail(email, code);
            } else {
                response = authService.verifyEmail(email, code);
            }
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            AuthResponse errorResponse = new AuthResponse();
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (simpleAuthService != null) {
                simpleAuthService.requestPasswordReset(email);
            } else {
                authService.requestPasswordReset(email);
            }
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
            if (simpleAuthService != null) {
                simpleAuthService.resetPassword(email, token, newPassword);
            } else {
                authService.resetPassword(email, token, newPassword);
            }
            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
