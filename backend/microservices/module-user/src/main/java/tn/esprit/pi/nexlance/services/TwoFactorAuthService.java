package tn.esprit.pi.nexlance.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nexlance.entities.User;
import tn.esprit.pi.nexlance.repositories.UserRepository;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class TwoFactorAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    private static final String APP_NAME = "NexLance";
    private static final int BACKUP_CODE_LENGTH = 8;
    private static final int BACKUP_CODE_COUNT = 10;

    /**
     * Configuration initiale du 2FA pour un utilisateur
     * Génère un secret TOTP et un QR code
     */
    @Transactional
    public Map<String, Object> setup2FA(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Ne pas permettre la reconfiguration si déjà activé
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new RuntimeException("Two-factor authentication is already enabled");
        }

        // Générer nouveau secret
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secret = secretGenerator.generate();

        // Créer QR code
        QrData qrData = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer(APP_NAME)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        QrGenerator qrGenerator = new ZxingPngQrGenerator();
        String qrCodeImage;
        try {
            byte[] imageData = qrGenerator.generate(qrData);
            qrCodeImage = Base64.getEncoder().encodeToString(imageData);
        } catch (QrGenerationException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }

        // Sauvegarder le secret (pas encore activé)
        user.setTwoFactorSecret(secret);
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("secret", secret);
        response.put("qrCode", "data:image/png;base64," + qrCodeImage);
        response.put("message", "Scan this QR code with your authenticator app");

        return response;
    }

    /**
     * Active le 2FA après vérification du premier code
     */
    @Transactional
    public Map<String, Object> enable2FA(UUID userId, String verificationCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getTwoFactorSecret() == null) {
            throw new RuntimeException("2FA setup not initiated. Please call setup endpoint first");
        }

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new RuntimeException("Two-factor authentication is already enabled");
        }

        // Vérifier le code
        if (!verifyTOTP(user.getTwoFactorSecret(), verificationCode)) {
            throw new RuntimeException("Invalid verification code");
        }

        // Générer codes de secours
        List<String> backupCodes = generateBackupCodes();
        List<String> hashedBackupCodes = backupCodes.stream()
                .map(passwordEncoder::encode)
                .collect(Collectors.toList());

        // Sauvegarder en JSON
        try {
            String backupCodesJson = objectMapper.writeValueAsString(hashedBackupCodes);
            user.setBackupCodes(backupCodesJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to save backup codes", e);
        }

        // Activer 2FA
        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Two-factor authentication enabled successfully");
        response.put("backupCodes", backupCodes);

        return response;
    }

    /**
     * Désactive le 2FA
     */
    @Transactional
    public Map<String, String> disable2FA(UUID userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Vérifier le mot de passe pour sécurité
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setBackupCodes(null);
        userRepository.save(user);

        return Map.of("message", "Two-factor authentication disabled successfully");
    }

    /**
     * Vérifie un code TOTP ou un code de secours
     */
    public boolean verify2FA(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new RuntimeException("Two-factor authentication is not enabled");
        }

        // Vérifier d'abord le TOTP
        if (verifyTOTP(user.getTwoFactorSecret(), code)) {
            return true;
        }

        // Si TOTP échoue, vérifier les codes de secours
        return verifyBackupCode(user, code);
    }

    /**
     * Régénère les codes de secours
     */
    @Transactional
    public Map<String, Object> regenerateBackupCodes(UUID userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new RuntimeException("Two-factor authentication is not enabled");
        }

        // Vérifier le mot de passe
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Générer nouveaux codes
        List<String> backupCodes = generateBackupCodes();
        List<String> hashedBackupCodes = backupCodes.stream()
                .map(passwordEncoder::encode)
                .collect(Collectors.toList());

        try {
            String backupCodesJson = objectMapper.writeValueAsString(hashedBackupCodes);
            user.setBackupCodes(backupCodesJson);
            userRepository.save(user);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to save backup codes", e);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Backup codes regenerated successfully");
        response.put("backupCodes", backupCodes);

        return response;
    }

    /**
     * Vérifie un code TOTP
     */
    private boolean verifyTOTP(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

        // Vérifier avec une fenêtre de tolérance de 1 (30 secondes avant/après)
        return verifier.isValidCode(secret, code);
    }

    /**
     * Vérifie un code de secours et le supprime s'il est valide
     */
    @Transactional
    protected boolean verifyBackupCode(User user, String code) {
        if (user.getBackupCodes() == null) {
            return false;
        }

        try {
            List<String> hashedBackupCodes = objectMapper.readValue(
                    user.getBackupCodes(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );

            // Trouver le code correspondant
            Optional<String> matchingCode = hashedBackupCodes.stream()
                    .filter(hashedCode -> passwordEncoder.matches(code, hashedCode))
                    .findFirst();

            if (matchingCode.isPresent()) {
                // Supprimer le code utilisé
                hashedBackupCodes.remove(matchingCode.get());
                String updatedCodesJson = objectMapper.writeValueAsString(hashedBackupCodes);
                user.setBackupCodes(updatedCodesJson);
                userRepository.save(user);
                return true;
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to process backup codes", e);
        }

        return false;
    }

    /**
     * Génère des codes de secours aléatoires
     */
    private List<String> generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        return IntStream.range(0, BACKUP_CODE_COUNT)
                .mapToObj(i -> {
                    StringBuilder code = new StringBuilder();
                    for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                        code.append(random.nextInt(10));
                    }
                    return code.toString();
                })
                .collect(Collectors.toList());
    }

    /**
     * Vérifie si un utilisateur a le 2FA activé
     */
    public boolean is2FAEnabled(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return Boolean.TRUE.equals(user.getTwoFactorEnabled());
    }
}
