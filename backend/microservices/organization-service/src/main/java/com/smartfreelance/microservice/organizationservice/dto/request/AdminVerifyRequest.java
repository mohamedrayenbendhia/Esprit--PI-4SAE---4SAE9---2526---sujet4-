package com.smartfreelance.microservice.organizationservice.dto.request;

import lombok.Data;

@Data
public class AdminVerifyRequest {
    /** APPROVE | REJECT | AWAITING_INFO — envoyé par le frontend */
    private String decision;
    /** Note admin — acceptée sous les deux noms pour compatibilité */
    private String adminNote;
    private String note;

    /** Retourne la note utilisable, quelle que soit la clé JSON envoyée */
    public String getEffectiveNote() {
        return adminNote != null ? adminNote : note;
    }
}
