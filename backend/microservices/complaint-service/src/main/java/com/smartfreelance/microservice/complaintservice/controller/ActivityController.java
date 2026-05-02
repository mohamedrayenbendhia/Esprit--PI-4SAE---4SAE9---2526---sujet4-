package com.smartfreelance.microservice.complaintservice.controller;

import com.smartfreelance.microservice.complaintservice.email.UserActivityTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint de ping pour le suivi de l'activité utilisateur.
 *
 * Le frontend Angular appelle PUT /api/complaints/activity/ping toutes les 2 minutes
 * pour signaler que l'utilisateur est actif sur la page réclamations.
 * Cela évite d'envoyer des emails quand l'utilisateur est déjà connecté.
 */
@RestController
@RequestMapping("/api/complaints/activity")
@RequiredArgsConstructor
@Slf4j
public class ActivityController {

    private final UserActivityTracker activityTracker;

    /**
     * Signal d'activité — appelé par le frontend toutes les 2 minutes.
     * Header X-User-Id injecté automatiquement par l'API Gateway.
     */
    @PutMapping("/ping")
    @PreAuthorize("hasAnyRole('FREELANCE', 'CLIENT', 'SUPPORT_AGENT', 'ADMIN')")
    public ResponseEntity<Map<String, String>> ping(
            @RequestHeader("X-User-Id") String userId) {

        activityTracker.recordActivity(userId);
        log.debug("[Activity] Ping reçu de l'utilisateur {}", userId);
        return ResponseEntity.ok(Map.of("status", "ok", "userId", userId));
    }
}