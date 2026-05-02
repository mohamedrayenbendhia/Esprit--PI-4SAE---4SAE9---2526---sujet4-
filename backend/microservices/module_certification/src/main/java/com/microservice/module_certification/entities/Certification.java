package com.microservice.module_certification.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "certifications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    @Builder.Default
    private boolean isExpired = false;

    private LocalDate expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean smsSent = false;

    @Column(nullable = false)
    private Long userSkillId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(nullable = false)
    private int score;

    private LocalDate date;
    private String certificateUrl;
}