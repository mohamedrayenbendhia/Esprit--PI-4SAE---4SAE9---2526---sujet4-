package com.smartfreelance.microservice.complaintservice.dto.advanced;

import com.smartfreelance.microservice.complaintservice.entity.MediationEvidence;
import com.smartfreelance.microservice.complaintservice.entity.MediationSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MediationSessionResponse {
    private String id;
    private String complaintId;
    private MediationSession.MediationStatus status;
    private LocalDateTime evidenceDeadline;
    private LocalDateTime decisionDeadline;
    private String openedByAdminId;
    private String decidedByAdminId;
    private MediationSession.MediationOutcome outcome;
    private String adminReasoning;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private List<EvidenceItem> evidences;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EvidenceItem {
        private String id;
        private String submittedByUserId;
        private MediationEvidence.PartyType partyType;
        private String description;
        private List<String> attachments;
        private LocalDateTime submittedAt;
    }
}
