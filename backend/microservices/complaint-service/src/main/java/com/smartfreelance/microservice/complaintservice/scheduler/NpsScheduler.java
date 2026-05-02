package com.smartfreelance.microservice.complaintservice.scheduler;

import com.smartfreelance.microservice.complaintservice.entity.NpsSurvey;
import com.smartfreelance.microservice.complaintservice.service.NpsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NpsScheduler {

    private final NpsService npsService;

    /**
     * Tous les jours à 09h00 : crée les enquêtes NPS pour les réclamations
     * clôturées depuis exactement 3 jours.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendNpsSurveys() {
        List<NpsSurvey> pending = npsService.getPendingSurveys();
        pending.forEach(s -> {
            npsService.createSurvey(s.getComplaintId(), s.getRespondentId());
            log.info("NPS survey queued for complaint {}", s.getComplaintId());
        });
        if (!pending.isEmpty())
            log.info("NPS scheduler: {} surveys created.", pending.size());
    }
}
