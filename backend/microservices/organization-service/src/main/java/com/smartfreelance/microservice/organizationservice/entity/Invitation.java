package com.smartfreelance.microservice.organizationservice.entity;

import com.smartfreelance.microservice.organizationservice.enums.InvitationStatus;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "org_invitations", indexes = {
        @Index(name = "idx_inv_org",     columnList = "organization_id"),
        @Index(name = "idx_inv_invitee", columnList = "invitee_id"),
        @Index(name = "idx_inv_status",  columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invitation {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(name = "inviter_id", nullable = false, length = 36)
    private String inviterId;

    @Column(name = "invitee_id", length = 36)
    private String inviteeId;

    @Column(name = "invitee_email", length = 255)
    private String inviteeEmail;

    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "token", unique = true, length = 64)
    private String token;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.token == null) this.token = UUID.randomUUID().toString().replace("-", "");
        if (this.expiresAt == null) this.expiresAt = LocalDateTime.now().plusDays(7);
    }
}
