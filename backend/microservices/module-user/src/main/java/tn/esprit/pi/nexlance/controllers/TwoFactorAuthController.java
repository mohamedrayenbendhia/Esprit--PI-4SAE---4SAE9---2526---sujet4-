package tn.esprit.pi.nexlance.controllers;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nexlance.services.TwoFactorAuthService;
import tn.esprit.pi.nexlance.services.UserService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Slf4j
public class TwoFactorAuthController {

    private final TwoFactorAuthService twoFactorAuthService;
    private final UserService userService;

    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setup2FA(Authentication authentication) {
        UUID userId = extractUserIdFromAuth(authentication);
        Map<String, Object> response = twoFactorAuthService.setup2FA(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/enable")
    public ResponseEntity<Map<String, Object>> enable2FA(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        UUID userId = extractUserIdFromAuth(authentication);
        String verificationCode = request.get("verificationCode");

        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Verification code is required"));
        }

        try {
            Map<String, Object> response = twoFactorAuthService.enable2FA(userId, verificationCode);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/disable")
    public ResponseEntity<Map<String, String>> disable2FA(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        UUID userId = extractUserIdFromAuth(authentication);
        String password = request.get("password");

        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }

        try {
            Map<String, String> response = twoFactorAuthService.disable2FA(userId, password);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify2FA(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        UUID userId = extractUserIdFromAuth(authentication);
        String code = request.get("code");

        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Code is required"));
        }

        try {
            boolean valid = twoFactorAuthService.verify2FA(userId, code);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", valid);
            response.put("message", valid ? "Code verified successfully" : "Invalid code");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/regenerate-backup-codes")
    public ResponseEntity<Map<String, Object>> regenerateBackupCodes(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        UUID userId = extractUserIdFromAuth(authentication);
        String password = request.get("password");

        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }

        try {
            Map<String, Object> response = twoFactorAuthService.regenerateBackupCodes(userId, password);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> get2FAStatus(Authentication authentication) {
        UUID userId = extractUserIdFromAuth(authentication);
        boolean enabled = twoFactorAuthService.is2FAEnabled(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", enabled);
        return ResponseEntity.ok(response);
    }

    private UUID extractUserIdFromAuth(Authentication authentication) {
        if (authentication != null) {
            Object details = authentication.getDetails();
            if (details instanceof Claims claims) {
                String userId = claims.get("userId", String.class);
                if (userId != null) return UUID.fromString(userId);
            }
            try {
                return UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException e) {
                log.debug("Subject is not a UUID, trying to lookup by email: {}", authentication.getName());
                return userService.getUserByEmail(authentication.getName()).getId();
            }
        }
        log.error("Unable to extract user ID from authentication");
        throw new RuntimeException("Unable to extract user ID from authentication");
    }
}