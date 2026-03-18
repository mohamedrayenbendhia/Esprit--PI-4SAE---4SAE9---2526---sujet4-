package com.esprit.microservice.contrat_backend.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String contractNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractType contractType;

    // INFO CLIENT
    @Column(nullable = false)
    private String clientId;
    private String clientName;
    private String clientCompany;
    private String clientEmail;
    private String clientPhone;
    private String clientAddress;
    private String clientCountry;
    private String clientSiret;

    // INFO FREELANCER
    @Column(nullable = false)
    private String freelancerId;
    private String freelancerCin;
    private String freelancerName;
    private String freelancerEmail;
    private String freelancerPhone;
    private String freelancerAddress;
    private String freelancerCountry;
    private String freelancerSiret;
    private String freelancerSpecialty;

    // MISSION
    @Column(nullable = false)
    private String missionTitle;
    @Column(columnDefinition = "TEXT")
    private String missionDescription;
    @Column(columnDefinition = "TEXT")
    private String deliverables;
    private String technologies;

    // DATES
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationMonths;

    // FINANCIER
    @Column(nullable = false)
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    private BigDecimal vatRate;
    private BigDecimal amountHT;
    private BigDecimal amountTTC;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    private String iban;
    private String bic;

    // JURIDIQUE
    @Enumerated(EnumType.STRING)
    private ApplicableLaw applicableLaw;
    private String competentCourt;
    private Integer confidentialityYears;
    private Boolean ipTransferToClient;
    private Boolean portfolioAllowed;
    private Integer portfolioDelayMonths;

    // STATUT & SIGNATURES
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status;
    private LocalDateTime clientSignedAt;
    private String clientSignatureHash;
    private LocalDateTime freelancerSignedAt;
    private String freelancerSignatureHash;

    // DEMANDE DE MODIFICATION
    @Column(columnDefinition = "TEXT")
    private String modificationComment;
    private LocalDateTime modificationRequestedAt;

    // METADATA
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // RELATIONS
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"contract"})
    private List<PaymentSchedule> paymentSchedules;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"contract"})
    private List<Milestone> milestones;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"contract"})
    private List<CustomClause> customClauses;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ContractStatus.DRAFT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}