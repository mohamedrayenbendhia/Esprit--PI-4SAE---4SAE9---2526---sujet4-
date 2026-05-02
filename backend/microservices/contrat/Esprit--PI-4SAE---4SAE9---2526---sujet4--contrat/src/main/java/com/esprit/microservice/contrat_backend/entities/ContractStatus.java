package com.esprit.microservice.contrat_backend.entities;

public enum ContractStatus {
    DRAFT,
    SIGNED_BY_CLIENT,
    PENDING_FREELANCER_SIGNATURE,
    FULLY_SIGNED,
    ACTIVE,
    COMPLETED,
    CANCELLED
}