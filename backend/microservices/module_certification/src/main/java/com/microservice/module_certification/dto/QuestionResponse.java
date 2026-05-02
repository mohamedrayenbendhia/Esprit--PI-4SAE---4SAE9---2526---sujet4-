package com.microservice.module_certification.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class QuestionResponse {
    private Long id;
    private String questionText;
    private String correctAnswer;
}