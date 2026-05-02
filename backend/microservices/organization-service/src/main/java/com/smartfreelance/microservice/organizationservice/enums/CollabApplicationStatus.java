package com.smartfreelance.microservice.organizationservice.enums;

public enum CollabApplicationStatus {
    PENDING,    // en attente de réponse de l'organisation
    ACCEPTED,   // accepté — collaboration démarrée
    REJECTED,   // refusé par l'organisation
    WITHDRAWN   // retiré par le candidat
}
