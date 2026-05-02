package com.microservice.module_certification.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_answers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_test_result_id", nullable = false)
    private UserTestResult userTestResult;

    @Column(nullable = false)
    private String answer;

    private boolean isCorrect;
}