package com.microservice.module_competence.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "endorsements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "freelancer_id", "skill_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Endorsement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String freelancerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime endorsedAt = LocalDateTime.now();
}