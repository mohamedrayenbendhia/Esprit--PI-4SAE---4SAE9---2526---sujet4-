package com.smartfreelance.microservice.complaintservice.dto.advanced;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NpsStatsResponse {
    private long totalSent;
    private long totalResponded;
    private double responseRate;
    private double averageScore;
    private long promoters;
    private long passives;
    private long detractors;
    private double npsScore;
}
