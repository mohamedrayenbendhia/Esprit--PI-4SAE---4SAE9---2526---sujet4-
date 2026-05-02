package com.smartfreelance.microservice.complaintservice.service;

import com.smartfreelance.microservice.complaintservice.dto.advanced.NpsResponseRequest;
import com.smartfreelance.microservice.complaintservice.dto.advanced.NpsStatsResponse;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import com.smartfreelance.microservice.complaintservice.entity.NpsSurvey;
import com.smartfreelance.microservice.complaintservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationEvent;
import com.smartfreelance.microservice.complaintservice.notification.ComplaintNotificationService;
import com.smartfreelance.microservice.complaintservice.repository.ComplaintRepository;
import com.smartfreelance.microservice.complaintservice.repository.NpsSurveyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NpsServiceImpl implements NpsService {

    private final NpsSurveyRepository          npsRepo;
    private final ComplaintRepository           complaintRepo;
    private final ComplaintNotificationService  notificationService;

    private static final long NPS_DELAY_DAYS = 3;

    @Override
    public NpsSurvey createSurvey(String complaintId, String respondentId) {
        if (npsRepo.existsByComplaintId(complaintId))
            return npsRepo.findByComplaintId(complaintId).get();

        NpsSurvey survey = NpsSurvey.builder()
                .complaintId(complaintId)
                .respondentId(respondentId)
                .build();
        NpsSurvey saved = npsRepo.save(survey);
        log.info("NPS survey created for complaint {} (respondent {})", complaintId, respondentId);

        // GAP #1c — inviter le reporter à donner son avis maintenant que la réclamation est clôturée
        complaintRepo.findById(complaintId).ifPresent(c ->
            notificationService.handle(ComplaintNotificationEvent.builder()
                    .eventType(ComplaintNotificationEvent.EventType.NPS_SURVEY_READY)
                    .complaintId(c.getId())
                    .ticketNumber(c.getTicketNumber())
                    .complaintSubject(c.getSubject())
                    .reporterId(respondentId)
                    .build())
        );

        return saved;
    }

    @Override
    public NpsSurvey respond(String complaintId, NpsResponseRequest req, String userId) {
        NpsSurvey survey = npsRepo.findByComplaintId(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("NPS survey not found for complaint: " + complaintId));

        if (!survey.getRespondentId().equals(userId))
            throw new IllegalArgumentException("Cet utilisateur n'est pas le destinataire de l'enquête.");
        if (survey.getRespondedAt() != null)
            throw new IllegalStateException("Vous avez déjà répondu à cette enquête.");

        survey.setScore(req.getScore());
        survey.setComment(req.getComment());
        survey.setRespondedAt(LocalDateTime.now());
        survey.setCategory(NpsSurvey.computeCategory(req.getScore()));

        return npsRepo.save(survey);
    }

    @Override
    @Transactional(readOnly = true)
    public NpsStatsResponse getStats() {
        long totalSent     = npsRepo.count();
        long totalResponded = npsRepo.countResponded();
        long promoters     = npsRepo.countPromoters();
        long detractors    = npsRepo.countDetractors();
        long passives      = totalResponded - promoters - detractors;
        Double avgScore    = npsRepo.computeAverageScore();

        double responseRate = totalSent > 0 ? (double) totalResponded / totalSent * 100 : 0;
        double npsScore     = totalResponded > 0
                ? (double) (promoters - detractors) / totalResponded * 100 : 0;

        return NpsStatsResponse.builder()
                .totalSent(totalSent)
                .totalResponded(totalResponded)
                .responseRate(Math.round(responseRate * 10.0) / 10.0)
                .averageScore(avgScore != null ? Math.round(avgScore * 10.0) / 10.0 : 0)
                .promoters(promoters)
                .passives(Math.max(passives, 0))
                .detractors(detractors)
                .npsScore(Math.round(npsScore * 10.0) / 10.0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NpsSurvey> getPendingSurveys() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(NPS_DELAY_DAYS);
        // Réclamations clôturées il y a exactement 3 jours sans enquête NPS
        return complaintRepo.findAll().stream()
                .filter(c -> c.getStatus() == Complaint.Status.CLOSED)
                .filter(c -> c.getClosedAt() != null)
                .filter(c -> c.getClosedAt().isBefore(threshold)
                          && c.getClosedAt().isAfter(threshold.minusDays(1)))
                .filter(c -> !npsRepo.existsByComplaintId(c.getId()))
                .map(c -> NpsSurvey.builder()
                        .complaintId(c.getId())
                        .respondentId(c.getReporterId())
                        .build())
                .toList();
    }
}
