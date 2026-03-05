package com.microservice.module_certification.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserAnswerResponse {
    private Long id;
    private String questionText;
    private String yourAnswer;
    private boolean isCorrect;
}