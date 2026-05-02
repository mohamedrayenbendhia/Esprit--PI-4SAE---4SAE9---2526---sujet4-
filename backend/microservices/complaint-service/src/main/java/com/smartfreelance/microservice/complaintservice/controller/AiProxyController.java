package com.smartfreelance.microservice.complaintservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Proxy côté serveur pour l'API Hugging Face Inference.
 *
 * Problème résolu : l'API HF ne renvoie pas de header CORS lorsque le
 * modèle est encore en cours de chargement (503) → le navigateur bloque
 * la requête avant même de recevoir la réponse.
 *
 * Solution : le frontend envoie {subject, description} à ce proxy,
 * qui construit la requête HF, ajoute l'Authorization et retourne
 * la réponse brute au frontend (qui garde toute la logique de mapping).
 *
 * Le token reste 100 % côté serveur.
 */
@RestController
@RequestMapping("/api/complaints/ai")
@RequiredArgsConstructor
@Slf4j
public class AiProxyController {

    private final ObjectMapper objectMapper;

    @Value("${hf.api-token:}")
    private String hfApiToken;

    @Value("${hf.model-url:https://router.huggingface.co/hf-inference/models/joeddav/xlm-roberta-large-xnli}")
    private String hfModelUrl;

    private static final List<String> CANDIDATE_LABELS = List.of(
            "le client refuse de payer ou le freelance n'a pas reçu sa rémunération après livraison",
            "le travail livré par le prestataire est incomplet, incorrect ou ne respecte pas le cahier des charges",
            "l'interlocuteur ne répond plus aux messages et est totalement injoignable depuis plusieurs jours",
            "une personne harcèle, insulte ou envoie des messages menaçants de façon répétée",
            "le freelance a reçu un acompte et a disparu sans livrer le moindre travail",
            "la plateforme elle-même a un bug ou une panne qui bloque l'accès à une fonctionnalité",
            "le litige ne correspond à aucune des catégories précédentes"
    );

    /**
     * POST /api/complaints/ai/suggest
     *
     * Body  : { "subject": "...", "description": "..." }
     * Retour : objet HF brut { labels, scores } — ou 204 si désactivé / modèle en chargement.
     */
    @PostMapping("/suggest")
    @PreAuthorize("hasAnyRole('CLIENT', 'FREELANCE')")
    public ResponseEntity<?> suggest(@RequestBody Map<String, String> body) {

        // ── 1. Token non configuré → désactivé ─────────────────────────
        if (hfApiToken == null || hfApiToken.isBlank()) {
            log.warn("[AI Proxy] Token HF absent ou vide (hf.api-token) — suggestion désactivée");
            return ResponseEntity.noContent().build();
        }
        log.info("[AI Proxy] Token HF présent ({}...) — appel HF en cours", hfApiToken.substring(0, Math.min(8, hfApiToken.length())));

        // ── 2. Texte trop court ─────────────────────────────────────────
        String subject     = body.getOrDefault("subject", "").trim();
        String description = body.getOrDefault("description", "").trim();
        String fullText    = (subject + ". " + description).trim();
        if (fullText.length() < 20) {
            return ResponseEntity.noContent().build();
        }
        String input = fullText.length() > 512 ? fullText.substring(0, 512) : fullText;

        // ── 3. Payload HF ───────────────────────────────────────────────
        Map<String, Object> hfPayload = Map.of(
                "inputs",     input,
                "parameters", Map.of(
                        "candidate_labels", CANDIDATE_LABELS,
                        "multi_label",      false
                )
        );

        // ── 4. Appel HF via proxy ───────────────────────────────────────
        try {
            String rawJson = WebClient.builder()
                    .build()
                    .post()
                    .uri(hfModelUrl)
                    .header("Authorization", "Bearer " + hfApiToken)
                    .header("Content-Type",  "application/json")
                    .bodyValue(hfPayload)
                    .exchangeToMono(response -> {
                        if (response.statusCode().is2xxSuccessful()) {
                            return response.bodyToMono(String.class);
                        }
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .doOnNext(errBody -> log.warn(
                                        "[AI Proxy] HF erreur {} — {}",
                                        response.statusCode(),
                                        errBody.length() > 300 ? errBody.substring(0, 300) : errBody))
                                .thenReturn("");
                    })
                    .timeout(Duration.ofSeconds(15))
                    .onErrorResume(ex -> {
                        log.warn("[AI Proxy] Erreur réseau/timeout : {} — {}",
                                ex.getClass().getSimpleName(), ex.getMessage());
                        return Mono.just("");
                    })
                    .block();

            if (rawJson == null || rawJson.isBlank()) {
                return ResponseEntity.noContent().build();
            }

            Object parsed = objectMapper.readValue(rawJson, Object.class);
            return ResponseEntity.ok(parsed);

        } catch (Exception ex) {
            log.warn("[AI Proxy] Erreur inattendue : {} — {}", ex.getClass().getSimpleName(), ex.getMessage());
            return ResponseEntity.noContent().build();
        }
    }

    /**
     * GET /api/complaints/ai/status
     * Indique au frontend si la suggestion IA est activée.
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('CLIENT', 'FREELANCE')")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of("enabled",
                hfApiToken != null && !hfApiToken.isBlank()));
    }
}
