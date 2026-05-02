package tn.esprit.pi.nexlance.mappers;

import tn.esprit.pi.nexlance.dto.KYCVerificationDto;
import tn.esprit.pi.nexlance.entities.KYCVerification;
import tn.esprit.pi.nexlance.entities.User;

public class KYCMapper {

    public static KYCVerificationDto toDto(KYCVerification verification) {
        if (verification == null) {
            return null;
        }

        KYCVerificationDto dto = new KYCVerificationDto();
        dto.setId(verification.getId());
        dto.setUserId(verification.getUserId());
        dto.setDocumentType(verification.getDocumentType());
        dto.setDocumentUrl(verification.getDocumentUrl());
        dto.setStatus(verification.getStatus());
        dto.setSubmittedAt(verification.getSubmittedAt());
        dto.setReviewedAt(verification.getReviewedAt());
        dto.setReviewedBy(verification.getReviewedBy());
        dto.setRejectionReason(verification.getRejectionReason());
        dto.setExpiryDate(verification.getExpiryDate());

        // Add user info if available
        if (verification.getUser() != null) {
            User user = verification.getUser();
            dto.setUserFirstName(user.getFirstName());
            dto.setUserLastName(user.getLastName());
            dto.setUserEmail(user.getEmail());
        }

        // Add reviewer info if available
        if (verification.getReviewer() != null) {
            User reviewer = verification.getReviewer();
            dto.setReviewerFirstName(reviewer.getFirstName());
            dto.setReviewerLastName(reviewer.getLastName());
        }

        return dto;
    }
}
