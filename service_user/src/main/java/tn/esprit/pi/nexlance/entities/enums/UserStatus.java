package tn.esprit.pi.nexlance.entities.enums;

/**
 * Énumération représentant les statuts possibles d'un compte utilisateur
 * 
 * @author NextLance Team
 * @version 1.0.0
 * @since 2026-02-15
 */
public enum UserStatus {
    /**
     * Compte actif - L'utilisateur peut se connecter et utiliser la plateforme
     */
    ACTIVE,
    
    /**
     * Compte suspendu - L'utilisateur ne peut pas se connecter
     */
    SUSPENDED,
    
    /**
     * En attente de vérification - Compte créé mais email non vérifié
     */
    PENDING_VERIFICATION,
    
    /**
     * Compte supprimé - Marqué pour suppression (soft delete)
     */
    DELETED
}
