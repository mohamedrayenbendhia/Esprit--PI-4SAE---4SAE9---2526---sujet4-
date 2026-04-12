package tn.esprit.pi.nexlance.services;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nexlance.dto.AuthResponse;
import tn.esprit.pi.nexlance.dto.LoginRequest;
import tn.esprit.pi.nexlance.dto.RegisterRequest;
import tn.esprit.pi.nexlance.entities.User;
import tn.esprit.pi.nexlance.repositories.UserRepository;
import tn.esprit.pi.nexlance.security.JwtUtil;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "false")
public class SimpleAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        try {
            // Create user in local database
            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setType(request.getType());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setStatus(User.UserStatus.ACTIVE);
            user.setSubscriptionType(User.SubscriptionType.FREE);
            user.setEmailVerified(true);

            User savedUser = userRepository.save(user);

            // Generate JWT token
            String token = jwtUtil.generateToken(
                savedUser.getEmail(),
                savedUser.getId(),
                savedUser.getType().toString()
            );

            return buildAuthResponse(savedUser, "Registration successful", token);
        } catch (Exception e) {
            throw new RuntimeException("Error during registration: " + e.getMessage(), e);
        }
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            // Find user in local database
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Invalid email or password"));

            // Check password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("Invalid email or password");
            }

            if (user.getStatus() == User.UserStatus.SUSPENDED) {
                throw new RuntimeException("Account is suspended");
            }

            if (user.getStatus() == User.UserStatus.DELETED) {
                throw new RuntimeException("Account not found");
            }

            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // Generate JWT token
            String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getId(),
                user.getType().toString()
            );

            return buildAuthResponse(user, "Login successful", token);
        } catch (Exception e) {
            throw new RuntimeException("Invalid email or password: " + e.getMessage(), e);
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        // For now, just return an error
        throw new RuntimeException("Refresh token not implemented in simple mode");
    }

    public AuthResponse verifyEmail(String email, String code) {
        // For now, mark all emails as verified on registration
        throw new RuntimeException("Email verification not required in simple mode");
    }

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Generate a random reset token (6 digit code for simplicity)
        String resetToken = String.format("%06d", new java.util.Random().nextInt(999999));
        
        // Set token expiration to 1 hour from now
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(java.time.LocalDateTime.now().plusHours(1));
        
        userRepository.save(user);
        
        // TODO: In production, send this token via email
        // For now, we'll just log it (NOT SECURE - only for development)
        System.out.println("Password reset token for " + email + ": " + resetToken);
        System.out.println("Token expires at: " + user.getResetTokenExpiry());
    }

    @Transactional
    public void resetPassword(String email, String token, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Validate token
        if (user.getResetToken() == null || !user.getResetToken().equals(token)) {
            throw new RuntimeException("Invalid reset token");
        }
        
        // Check if token has expired
        if (user.getResetTokenExpiry() == null || 
            java.time.LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            throw new RuntimeException("Reset token has expired");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        
        // Clear reset token
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user, String message, String token) {
        // Mapper UserType vers le format role attendu par le frontend
        String role = switch (user.getType()) {
            case FREELANCE -> "FREELANCER";
            case CLIENT -> "CLIENT";
            case ADMIN -> "ADMIN";
        };
        
        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setType(user.getType());
        response.setRole(role); // Nouveau champ pour la compatibilité frontend
        response.setStatus(user.getStatus());
        response.setEmailVerified(user.getEmailVerified());
        response.setToken(token);
        response.setMessage(message);
        response.setAvatar(user.getAvatar());
        return response;
    }
}
