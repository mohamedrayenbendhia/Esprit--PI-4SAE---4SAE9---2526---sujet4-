package com.microservice.module_competence.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class SkillRequest {

    @NotBlank(message = "Skill name is required")
    private String name;
}
