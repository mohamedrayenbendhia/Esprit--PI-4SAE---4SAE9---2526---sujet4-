package com.microservice.module_competence.dto;

import com.microservice.module_competence.enums.Level;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserSkillResponse {
    private Long id;
    private String userId; // ✅ UUID en String
    private SkillResponse skill;
    private Level level;
    private int yearsOfExperience;
}