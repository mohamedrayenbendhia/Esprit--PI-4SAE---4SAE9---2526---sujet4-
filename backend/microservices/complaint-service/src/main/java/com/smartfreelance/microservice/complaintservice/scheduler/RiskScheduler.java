package com.smartfreelance.microservice.complaintservice.scheduler;

import com.smartfreelance.microservice.complaintservice.service.RiskScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskScheduler {

    private final RiskScoringService riskService;

    /** Recalcule tous les scores de risque chaque nuit à 02h00. */
    @Scheduled(cron = "0 0 2 * * *")
    public void recalculateRiskScores() {
        log.info("Risk score recalculation starting...");
        riskService.recalculateAll();
        log.info("Risk score recalculation completed.");
    }
}
