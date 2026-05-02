package tn.esprit.pi.nexlance.entities.enums;

/**
 * Énumération représentant les types d'utilisateurs dans la plateforme NextLance
 * 
 * @author NextLance Team
 * @version 1.0.0
 * @since 2026-02-15
 */
public enum UserType {
    /**
     * Utilisateur client - Peut poster des projets et embaucher des freelances
     */
    CLIENT,

    /**
     * Utilisateur freelance - Peut postuler à des projets et fournir des services
     */
    FREELANCE,

    /**
     * Utilisateur administrateur - Accès complet à toutes les fonctionnalités
     */
    ADMIN,

    /**
     * Agent de support - Traite et résout les réclamations des utilisateurs
     */
    SUPPORT_AGENT
}
