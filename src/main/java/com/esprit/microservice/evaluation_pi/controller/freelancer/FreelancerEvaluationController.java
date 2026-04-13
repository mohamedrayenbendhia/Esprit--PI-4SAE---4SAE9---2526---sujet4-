package com.esprit.microservice.evaluation_pi.controller.freelancer;

import com.esprit.microservice.evaluation_pi.entities.Evaluation;
import com.esprit.microservice.evaluation_pi.services.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/freelancer/evaluations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class FreelancerEvaluationController {

    private final EvaluationService evaluationService;
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // --- CONSULTATION ---

    @GetMapping("/my-evaluations")
    public ResponseEntity<List<Evaluation>> getMyEvaluations(@RequestParam String email) {
        return ResponseEntity.ok(evaluationService.getEvaluationsByEvaluatedId(email.toLowerCase()));
    }

    @GetMapping("/count/{email}")
    public ResponseEntity<Long> countEvaluations(@PathVariable String email) {
        return ResponseEntity.ok((long) evaluationService.getEvaluationsByEvaluatedId(email.toLowerCase()).size());
    }

    @GetMapping("/average-rating/{email}")
    public ResponseEntity<Double> averageRating(@PathVariable String email) {
        double avg = evaluationService.getEvaluationsByEvaluatedId(email.toLowerCase())
                .stream().filter(e -> e.getRatingGlobal() != null)
                .mapToDouble(Evaluation::getRatingGlobal).average().orElse(0.0);
        return ResponseEntity.ok(Math.round(avg * 10.0) / 10.0);
    }

    @GetMapping("/{evaluationId}")
    public ResponseEntity<?> getEvaluationDetails(@PathVariable Long evaluationId, @RequestParam String freelancerEmail) {
        Evaluation e = evaluationService.getEvaluationById(evaluationId);
        if (!e.getEvaluatedId().equalsIgnoreCase(freelancerEmail)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(e);
    }

    // --- ACTIONS ---

    @PostMapping("/evaluation/{id}/respond")
    public ResponseEntity<?> respondToEvaluation(@PathVariable Long id, @RequestBody Map<String, String> body, @RequestParam String freelancerEmail) {
        Evaluation updated = evaluationService.respondToEvaluation(id, body.get("response"));
        pushStatsUpdate(freelancerEmail);
        return ResponseEntity.ok(updated);
    }

    //  Évaluer un client (freelancer)
    @PostMapping("/evaluate-client/{clientEmail}")
    public ResponseEntity<?> evaluateClient(
            @PathVariable String clientEmail,
            @RequestBody Evaluation evaluation,
            @RequestParam String freelancerId,
            @RequestParam(required = false) String projectId) {

        if (clientEmail == null || freelancerId == null) {
            return ResponseEntity.badRequest().body("Email du client et freelancerId requis");
        }
        if (!clientEmail.contains("@")) {
            return ResponseEntity.badRequest().body("Email invalide");
        }

        evaluation.setEvaluatedId(clientEmail);
        evaluation.setEvaluatorId(freelancerId);
        evaluation.setProjectId(projectId);

        return ResponseEntity.ok(evaluationService.createEvaluation(evaluation));
    }

    //  Liste des évaluations données par un freelancer (clients)
    @GetMapping("/freelancer/{freelancerId}/given")
    public ResponseEntity<List<Evaluation>> getMyGivenEvaluations(@PathVariable String freelancerId) {
        return ResponseEntity.ok(evaluationService.getEvaluationsByEvaluatorId(freelancerId));
    }

    // --- SSE & STATS ---

    @GetMapping(value = "/stats/{email}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamStats(@PathVariable String email) {
        SseEmitter emitter = new SseEmitter(1800000L);
        List<SseEmitter> list = emitters.computeIfAbsent(email.toLowerCase(), k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        emitter.onCompletion(() -> list.remove(emitter));
        emitter.onTimeout(() -> list.remove(emitter));
        pushStatsUpdate(email);
        return emitter;
    }

    public void pushStatsUpdate(String email) {
        List<SseEmitter> list = emitters.get(email.toLowerCase());
        if (list == null || list.isEmpty()) return;
        Map<String, Object> stats = buildDetailedStats(email);
        list.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("stats").data(stats));
                return false;
            } catch (Exception e) { return true; }
        });
    }

    private Map<String, Object> buildDetailedStats(String email) {
        List<Evaluation> evals = evaluationService.getEvaluationsByEvaluatedId(email.toLowerCase());
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalEvaluations", evals.size());
        stats.put("averageRating", Math.round(evals.stream().filter(e -> e.getRatingGlobal() != null).mapToDouble(Evaluation::getRatingGlobal).average().orElse(0.0) * 10.0) / 10.0);

        Map<String, Long> dist = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            final int s = i;
            dist.put(String.valueOf(s), evals.stream().filter(e -> e.getRatingGlobal() != null && Math.round(e.getRatingGlobal()) == s).count());
        }
        stats.put("distribution", dist);

        Map<String, Double> cats = new HashMap<>();
        cats.put("quality", roundAvg(evals, "quality"));
        cats.put("deadline", roundAvg(evals, "deadline"));
        cats.put("communication", roundAvg(evals, "comm"));
        cats.put("professionalism", roundAvg(evals, "prof"));
        stats.put("categoryAverages", cats);
        stats.put("recentEvaluations", evals);
        return stats;
    }

    private double roundAvg(List<Evaluation> list, String type) {
        return Math.round(list.stream().mapToDouble(e -> {
            if (type.equals("quality")) return e.getQualityScore() != null ? e.getQualityScore() : 0;
            if (type.equals("deadline")) return e.getDeadlineScore() != null ? e.getDeadlineScore() : 0;
            if (type.equals("comm")) return e.getCommunicationScore() != null ? e.getCommunicationScore() : 0;
            return e.getProfessionalismScore() != null ? e.getProfessionalismScore() : 0;
        }).average().orElse(0.0) * 10.0) / 10.0;
    }
}