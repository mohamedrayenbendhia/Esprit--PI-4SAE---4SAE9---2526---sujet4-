package com.microservice.module_certification.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TestRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "skillId is required")
    private Long skillId;

    @Min(value = 1, message = "Passing score must be at least 1")
    @Max(value = 100, message = "Passing score cannot exceed 100")
    private int passingScore;

    private List<QuestionRequest> questions;
}