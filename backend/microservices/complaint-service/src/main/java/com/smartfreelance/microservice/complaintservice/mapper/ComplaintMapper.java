package com.smartfreelance.microservice.complaintservice.mapper;

import com.smartfreelance.microservice.complaintservice.dto.ComplaintDTO;
import com.smartfreelance.microservice.complaintservice.dto.ComplaintRequestDTO;
import com.smartfreelance.microservice.complaintservice.dto.ComplaintUpdateDTO;
import com.smartfreelance.microservice.complaintservice.entity.Complaint;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ComplaintMapper {

    /**
     * Convertit une entité Complaint en DTO pour la réponse
     */
    public ComplaintDTO toDTO(Complaint complaint) {
        if (complaint == null) {
            return null;
        }

        return ComplaintDTO.builder()
                .id(complaint.getId())
                .ticketNumber(complaint.getTicketNumber())
                .reporterId(complaint.getReporterId())
                .reportedUserId(complaint.getReportedUserId())
                .projectId(complaint.getProjectId())
                .category(complaint.getCategory())
                .priority(complaint.getPriority())
                .status(complaint.getStatus())
                .subject(complaint.getSubject())
                .description(complaint.getDescription())
                .attachments(complaint.getAttachments())
                .assignedToId(complaint.getAssignedToId())
                .resolution(complaint.getResolution())
                .resolutionType(complaint.getResolutionType())
                .satisfactionRating(complaint.getSatisfactionRating())
                .createdAt(complaint.getCreatedAt())
                .firstResponseAt(complaint.getFirstResponseAt())
                .resolvedAt(complaint.getResolvedAt())
                .closedAt(complaint.getClosedAt())
                .updatedAt(complaint.getUpdatedAt())
                .build();
    }

    /**
     * Convertit une liste d'entités en liste de DTOs
     */
    public List<ComplaintDTO> toDTOList(List<Complaint> complaints) {
        if (complaints == null) {
            return null;
        }

        return complaints.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convertit un ComplaintRequestDTO en entité pour la création
     */
    public Complaint toEntity(ComplaintRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        Complaint complaint = new Complaint();
        complaint.setReporterId(dto.getReporterId());
        // reportedUserId résolu depuis reportedUserEmail par le controller — ne pas mapper ici
        complaint.setProjectId(dto.getProjectId());
        complaint.setCategory(dto.getCategory());
        complaint.setPriority(dto.getPriority());
        complaint.setSubject(dto.getSubject());
        complaint.setDescription(dto.getDescription());
        complaint.setAttachments(dto.getAttachments());

        return complaint;
    }

    /**
     * Met à jour une entité existante avec les données du ComplaintUpdateDTO
     */
    public void updateEntityFromDTO(Complaint complaint, ComplaintUpdateDTO dto) {
        if (dto == null || complaint == null) {
            return;
        }

        if (dto.getSubject() != null) {
            complaint.setSubject(dto.getSubject());
        }
        if (dto.getDescription() != null) {
            complaint.setDescription(dto.getDescription());
        }
        if (dto.getCategory() != null) {
            complaint.setCategory(dto.getCategory());
        }
        if (dto.getPriority() != null) {
            complaint.setPriority(dto.getPriority());
        }
        if (dto.getAttachments() != null) {
            complaint.setAttachments(dto.getAttachments());
        }
    }
}