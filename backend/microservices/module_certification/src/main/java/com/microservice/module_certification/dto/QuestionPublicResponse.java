package com.microservice.module_certification.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class QuestionPublicResponse {
    private Long id;
    private String questionText;
}