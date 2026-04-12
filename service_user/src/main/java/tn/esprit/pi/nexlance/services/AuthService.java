package tn.esprit.pi.nexlance.services;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tn.esprit.pi.nexlance.dto.AuthResponse;
import tn.esprit.pi.nexlance.dto.LoginRequest;
import tn.esprit.pi.nexlance.dto.RegisterRequest;
import tn.esprit.pi.nexlance.entities.User;
import tn.esprit.pi.nexlance.repositories.UserRepository;
import tn.esprit.pi.nexlance.security.JwtUtil;

import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private Keycloak keycloak;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Value("${keycloak.enabled:false}")
    private boolean keycloakEnabled;

    @Value("${keycloak.realm:}")
    private String realm;

    @Value("${keycloak.auth-server-url:}")
    private String authServerUrl;

    @Value("${keycloak.resource:}")
    private String clientId;

    @Value("${keycloak.credentials.secret:}")
    private String clientSecret;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

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
            String keycloakUserId = null;
            
            // Create user in Keycloak if enabled
            if (keycloakEnabled && keycloak != null) {
                keycloakUserId = createKeycloakUser(request);
            }

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
            user.setKeycloakUserId(keycloakUserId);

            User savedUser = userRepository.save(user);

            // Generate JWT token if Keycloak is not enabled
            String token = null;
            if (!keycloakEnabled) {
                token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getId(), savedUser.getType().toString());
            }

            AuthResponse response = buildAuthResponse(savedUser, "Registration successful", token);
            return response;
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

            if (user.getStatus() == User.UserStatus.SUSPENDED) {
                throw new RuntimeException("Account is suspended");
            }

            if (user.getStatus() == User.UserStatus.DELETED) {
                throw new RuntimeException("Account not found");
            }

            String accessToken;
            String refreshToken = null;

            if (keycloakEnabled && keycloak != null && restTemplate != null) {
                // Authenticate with Keycloak and get token
                Map<String, Object> tokenResponse = getKeycloakToken(request.getEmail(), request.getPassword());
                accessToken = (String) tokenResponse.get("access_token");
                refreshToken = (String) tokenResponse.get("refresh_token");
            } else {
                // Simple authentication with password check
                if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                    throw new RuntimeException("Invalid email or password");
                }
                // Generate JWT token
                accessToken = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getType().toString());
            }

            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            
            AuthResponse response = buildAuthResponse(user, "Login successful", accessToken);
            response.setRefreshToken(refreshToken);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Invalid email or password: " + e.getMessage(), e);
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!keycloakEnabled || keycloak == null || restTemplate == null) {
            throw new RuntimeException("Token refresh not supported in simple mode. Please login again.");
        }
        
        try {
            Map<String, Object> tokenResponse = refreshKeycloakToken(refreshToken);
            
            String accessToken = (String) tokenResponse.get("access_token");
            String newRefreshToken = (String) tokenResponse.get("refresh_token");
            
            AuthResponse response = new AuthResponse();
            response.setToken(accessToken);
            response.setRefreshToken(newRefreshToken);
            response.setMessage("Token refreshed successfully");
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Error refreshing token: " + e.getMessage(), e);
        }
    }

    private String createKeycloakUser(RegisterRequest request) {
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        // Create user representation
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEnabled(true);
        userRepresentation.setUsername(request.getEmail());
        userRepresentation.setEmail(request.getEmail());
        userRepresentation.setFirstName(request.getFirstName());
        userRepresentation.setLastName(request.getLastName());
        userRepresentation.setEmailVerified(true);

        // Set attributes
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("phoneNumber", Collections.singletonList(request.getPhoneNumber()));
        attributes.put("userType", Collections.singletonList(request.getType().toString()));
        userRepresentation.setAttributes(attributes);

        // Create user
        Response response = usersResource.create(userRepresentation);

        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatusInfo());
        }

        // Extract user ID from location header
        String locationHeader = response.getHeaderString("Location");
        String userId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);

        // Set password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);

        usersResource.get(userId).resetPassword(credential);

        response.close();
        return userId;
    }

    private Map<String, Object> getKeycloakToken(String username, String password) {
        String tokenUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("grant_type", "password");
        map.add("username", username);
        map.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                Map.class
        );

        return response.getBody();
    }

    private Map<String, Object> refreshKeycloakToken(String refreshToken) {
        String tokenUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("grant_type", "refresh_token");
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                Map.class
        );

        return response.getBody();
    }

    @Transactional
    public AuthResponse verifyEmail(String email, String verificationCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmailVerified(true);
        if (user.getStatus() == User.UserStatus.PENDING_VERIFICATION) {
            user.setStatus(User.UserStatus.ACTIVE);
        }

        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser, "Email verified successfully", null);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (keycloakEnabled && keycloak != null) {
            // With Keycloak, password reset is typically handled through Keycloak's UI
            // You can also use Keycloak Admin API to send reset password email
            throw new RuntimeException("Password reset should be done through Keycloak");
        } else {
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
    }

    @Transactional
    public void resetPassword(String email, String resetToken, String newPassword) {
        if (keycloakEnabled && keycloak != null) {
            // With Keycloak, password reset is typically handled through Keycloak
            throw new RuntimeException("Password reset should be done through Keycloak");
        } else {
            // In simple mode, validate reset token and update password
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate token
            if (user.getResetToken() == null || !user.getResetToken().equals(resetToken)) {
                throw new RuntimeException("Invalid reset token");
            }
            
            // Check if token has expired
            if (user.getResetTokenExpiry() == null || 
                java.time.LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
                throw new RuntimeException("Reset token has expired");
            }
            
            user.setPassword(passwordEncoder.encode(newPassword));
            
            // Clear reset token
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            
            userRepository.save(user);
        }
    }

    private AuthResponse buildAuthResponse(User user, String message, String token) {
        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setType(user.getType());
        response.setStatus(user.getStatus());
        response.setEmailVerified(user.getEmailVerified());
        response.setToken(token);
        response.setMessage(message);
        response.setAvatar(user.getAvatar());
        return response;
    }
}
