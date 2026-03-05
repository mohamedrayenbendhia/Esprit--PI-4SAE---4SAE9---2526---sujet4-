package com.microservice.module_certification.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_test_results")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(nullable = false)
    private Long userSkillId;

    private int score;
    private boolean isPassed;
    private LocalDateTime passedAt;

    @OneToMany(mappedBy = "userTestResult", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserAnswer> answers = new ArrayList<>();
}