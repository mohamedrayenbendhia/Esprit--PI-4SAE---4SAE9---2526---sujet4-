package com.smartfreelance.microservice.complaintservice.dto;

import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload pour la résolution d'une réclamation.
 * Remplace les @RequestParam pour éviter la troncature du texte de résolution
 * en query string et son exposition dans les logs d'accès.
 */
@Data
public class ResolveComplaintRequest {

    @NotBlank(message = "Le texte de résolution est obligatoire")
    private String resolution;

    @NotNull(message = "Le type de résolution est obligatoire")
    private Complaint.ResolutionType resolutionType;
}
