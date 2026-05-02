package com.smartfreelance.microservice.complaintservice.email;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Suivi de l'activité des utilisateurs pour la logique anti-spam emails.
 *
 * Règle : un email n'est envoyé pour un nouveau message que si l'utilisateur
 * est considéré comme inactif (pas d'activité depuis INACTIVITY_THRESHOLD_MS).
 *
 * Les utilisateurs signalent leur activité en appelant :
 *   PUT /api/complaints/activity/ping   (depuis le frontend, toutes les 2 minutes)
 *
 * En mémoire — adapté pour un déploiement mono-instance.
 * Pour un déploiement multi-instance, remplacer par Redis.
 */
@Component
public class UserActivityTracker {

    /**
     * Délai d'inactivité : si l'utilisateur n'a pas pingué depuis ce délai,
     * il est considéré comme inactif et recevra l'email.
     * Valeur : 8 minutes (spec : 5–10 minutes).
     */
    private static final long INACTIVITY_THRESHOLD_MS = 8 * 60 * 1000L;

    /** userId → timestamp du dernier ping */
    private final ConcurrentHashMap<String, Long> lastSeen = new ConcurrentHashMap<>();

    /**
     * Enregistre l'activité d'un utilisateur.
     * Appelé depuis ActivityController (ping frontend).
     */
    public void recordActivity(String userId) {
        if (userId != null && !userId.isBlank()) {
            lastSeen.put(userId, Instant.now().toEpochMilli());
        }
    }

    /**
     * Retourne true si l'utilisateur est considéré comme inactif.
     * Critères :
     *  - Jamais vu (pas encore pingué) → inactif
     *  - Dernier ping > INACTIVITY_THRESHOLD_MS → inactif
     */
    public boolean isInactive(String userId) {
        if (userId == null) return true;
        Long last = lastSeen.get(userId);
        if (last == null) return true;
        return (Instant.now().toEpochMilli() - last) > INACTIVITY_THRESHOLD_MS;
    }

    /**
     * Retourne le timestamp du dernier ping (en ms), ou 0 si jamais vu.
     */
    public long getLastSeenMs(String userId) {
        return lastSeen.getOrDefault(userId, 0L);
    }
}