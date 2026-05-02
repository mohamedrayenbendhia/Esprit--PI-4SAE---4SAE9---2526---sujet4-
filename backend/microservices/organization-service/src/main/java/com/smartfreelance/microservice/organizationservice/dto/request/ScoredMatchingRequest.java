package com.smartfreelance.microservice.organizationservice.dto.request;

import com.smartfreelance.microservice.organizationservice.enums.OrganizationSize;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationType;
import lombok.Data;

import java.util.List;

/**
 * Requête de matching scoré : le freelance fournit son profil,
 * l'algorithme retourne les organisations triées par score de compatibilité.
 */
@Data
public class ScoredMatchingRequest {

    /** Compétences du freelance (ex: ["React", "Node.js", "TypeScript"]) */
    private List<String> freelancerSkills;

    /** Localisation du freelance (ex: "Paris", "Lyon") */
    private String freelancerLocation;

    /** Type d'organisation préféré (facultatif) */
    private OrganizationType preferredType;

    /** Taille d'organisation préférée (facultatif — non scoré mais filtrant si fourni) */
    private OrganizationSize preferredSize;

    /**
     * Score minimum retenu (0–100). Permet d'éliminer les résultats trop faibles.
     * Valeur par défaut : 0 (tout retourner).
     */
    private double minScore = 0;

    /** Nombre maximum de résultats (défaut 20, max 50). */
    private int limit = 20;
}
