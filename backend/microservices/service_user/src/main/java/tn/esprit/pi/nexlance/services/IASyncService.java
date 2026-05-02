package tn.esprit.pi.nexlance.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.pi.nexlance.entities.FreelanceProfile;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class IASyncService {

    private final RestTemplate restTemplate = new RestTemplate();

    // On passe par la Gateway sur le port 8080
    private final String IA_SERVICE_URL = "http://localhost:8080/api/recommendations/sync/freelancer";

    public void syncProfile(FreelanceProfile profile) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", profile.getId().toString());

            // On transforme la liste [Java, Spring] en String "Java, Spring" pour ton Python
            String skillsStr = profile.getSkills() != null ? String.join(", ", profile.getSkills()) : "";
            payload.put("skills", skillsStr);

            payload.put("experienceYears", profile.getExperienceYears());
            payload.put("hourlyRate", profile.getHourlyRate());
            payload.put("rating", 5.0); // Valeur par défaut si tu n'as pas encore de rating

            restTemplate.postForEntity(IA_SERVICE_URL, payload, String.class);
            log.info("✅ Profil synchronisé avec le service IA pour l'utilisateur : {}", profile.getId());

        } catch (Exception e) {
            log.error("❌ Échec de la synchronisation IA : {}", e.getMessage());
            // On ne bloque pas l'utilisateur si l'IA est en panne
        }
    }
}