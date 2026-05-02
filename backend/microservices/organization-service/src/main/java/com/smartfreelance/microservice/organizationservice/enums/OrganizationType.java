package com.smartfreelance.microservice.organizationservice.enums;

/**
 * Types d'organisations autorisés sur la plateforme.
 * Uniquement des entités côté offre (groupes de freelances).
 * Les clients entreprises gèrent leur identité via leur profil utilisateur.
 */
public enum OrganizationType {
    AGENCY,          // Agence de freelances (structure commerciale)
    STARTUP,         // Startup tech fondée par des freelances
    ASSOCIATION,     // Collectif associatif
    FREELANCE_COOP   // Coopérative de freelances
}
