package com.microservice.module_certification.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TestResponse {
    private Long id;
    private String title;
    private Long skillId;
    private int passingScore;
    private List<QuestionResponse> questions;
}