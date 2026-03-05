package com.microservice.module_competence.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SkillResponse {
    private Long id;
    private String name;
}
