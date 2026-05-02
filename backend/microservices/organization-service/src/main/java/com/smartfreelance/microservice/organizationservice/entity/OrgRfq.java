package com.smartfreelance.microservice.organizationservice.entity;

import com.smartfreelance.microservice.organizationservice.enums.RfqStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "org_rfq")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrgRfq {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(name = "requester_id", nullable = false, length = 36)
    private String requesterId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "budget_min", precision = 12, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 12, scale = 2)
    private BigDecimal budgetMax;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "skills_needed", columnDefinition = "JSON")
    @Builder.Default
    private List<String> skillsNeeded = new ArrayList<>();

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RfqStatus status = RfqStatus.PENDING;

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    @Column(name = "responded_by_id", length = 36)
    private String respondedById;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}
