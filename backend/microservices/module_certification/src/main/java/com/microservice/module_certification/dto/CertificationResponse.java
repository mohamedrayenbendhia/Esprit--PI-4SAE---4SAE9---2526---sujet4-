package com.microservice.module_certification.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CertificationResponse {
    private Long id;
    private String userId;
    private Long userSkillId;
    private String testTitle;
    private int score;
    private LocalDate date;
    private String certificateUrl;
}