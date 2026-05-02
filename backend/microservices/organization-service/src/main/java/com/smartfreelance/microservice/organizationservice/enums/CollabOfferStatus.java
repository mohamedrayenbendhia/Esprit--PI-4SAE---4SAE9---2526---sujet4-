package com.smartfreelance.microservice.organizationservice.enums;

public enum CollabOfferStatus {
    OPEN,       // visible, accepte des candidatures
    CLOSED,     // n'accepte plus de candidatures (quota atteint ou décision manuelle)
    CANCELLED   // annulé par l'organisation
}
