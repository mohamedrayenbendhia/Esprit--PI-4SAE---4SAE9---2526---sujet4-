package com.microservice.module_certification.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserTestResultResponse {
    private Long id;
    private String userId;
    private Long testId;
    private String testTitle;
    private Long userSkillId;
    private int score;
    private int passingScore;
    private boolean isPassed;
    private LocalDateTime passedAt;
    private String cooldownMessage;
    private boolean onCooldown;
    private List<UserAnswerResponse> answers;
}