package tn.esprit.pi.nexlance.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "keycloak_user_id")
    private String keycloakUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType type;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionType subscriptionType = SubscriptionType.FREE;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Column(nullable = false)
    private Boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    @Column(name = "backup_codes", columnDefinition = "JSON")
    private String backupCodes;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;

    public enum UserType {
        FREELANCE,
        CLIENT,
        ADMIN
    }

    public enum UserStatus {
        ACTIVE,
        SUSPENDED,
        PENDING_VERIFICATION,
        DELETED
    }

    public enum SubscriptionType {
        FREE,
        PREMIUM,
        ENTERPRISE
    }
}
