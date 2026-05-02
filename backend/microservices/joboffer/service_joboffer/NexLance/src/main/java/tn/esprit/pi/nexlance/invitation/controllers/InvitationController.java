package tn.esprit.pi.nexlance.invitation.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nexlance.invitation.dto.CreateInvitationDto;
import tn.esprit.pi.nexlance.invitation.dto.UpdateInvitationDto;
import tn.esprit.pi.nexlance.invitation.entities.Invitation;
import tn.esprit.pi.nexlance.invitation.enums.InvitationStatus;
import tn.esprit.pi.nexlance.invitation.services.InvitationService;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InvitationController {
    
    private final InvitationService invitationService;
    
    /**
     * Créer une nouvelle invitation
     */
    @PostMapping
    public ResponseEntity<?> createInvitation(@RequestBody CreateInvitationDto dto) {
        try {
            System.out.println("📥 Received invitation request: " + dto);
            Invitation invitation = invitationService.createInvitation(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(invitation);
        } catch (RuntimeException e) {
            System.err.println("❌ Error creating invitation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Récupérer toutes les invitations
     */
    @GetMapping
    public ResponseEntity<List<Invitation>> getAllInvitations(
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String freelanceId,
            @RequestParam(required = false) String jobOfferId,
            @RequestParam(required = false) InvitationStatus status) {
        
        List<Invitation> invitations;
        
        if (clientId != null && !clientId.isEmpty()) {
            invitations = invitationService.getInvitationsByClientId(clientId);
        } else if (freelanceId != null && !freelanceId.isEmpty()) {
            invitations = invitationService.getInvitationsByFreelanceId(freelanceId);
        } else if (jobOfferId != null && !jobOfferId.isEmpty()) {
            invitations = invitationService.getInvitationsByJobOfferId(jobOfferId);
        } else if (status != null) {
            invitations = invitationService.getInvitationsByStatus(status);
        } else {
            invitations = invitationService.getAllInvitations();
        }
        
        return ResponseEntity.ok(invitations);
    }
    
    /**
     * Récupérer une invitation par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Invitation> getInvitationById(@PathVariable Long id) {
        try {
            Invitation invitation = invitationService.getInvitationById(id);
            return ResponseEntity.ok(invitation);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Mettre à jour le statut d'une invitation
     */
    @PutMapping("/{id}")
    public ResponseEntity<Invitation> updateInvitationStatus(
            @PathVariable Long id,
            @RequestBody UpdateInvitationDto dto) {
        try {
            Invitation invitation = invitationService.updateInvitationStatus(id, dto);
            return ResponseEntity.ok(invitation);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Supprimer une invitation
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvitation(@PathVariable Long id) {
        try {
            invitationService.deleteInvitation(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Expirer les invitations dépassées
     */
    @PostMapping("/expire-old")
    public ResponseEntity<Void> expireOldInvitations() {
        invitationService.expireOldInvitations();
        return ResponseEntity.ok().build();
    }
    
    /**
     * Récupérer les invitations reçues par un freelance
     * GET /api/invitations/received?freelanceId={id}
     */
    @GetMapping("/received")
    public ResponseEntity<?> getReceivedInvitations(@RequestParam(required = false) String freelanceId) {
        try {
            if (freelanceId == null || freelanceId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(java.util.Map.of("error", "freelanceId parameter is required"));
            }
            List<Invitation> invitations = invitationService.getInvitationsByFreelanceId(freelanceId);
            return ResponseEntity.ok(invitations);
        } catch (RuntimeException e) {
            System.err.println("❌ Error fetching received invitations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Récupérer les invitations envoyées par un client
     * GET /api/invitations/my-invitations?clientId={id}
     */
    @GetMapping("/my-invitations")
    public ResponseEntity<?> getMyInvitations(@RequestParam(required = false) String clientId) {
        try {
            if (clientId == null || clientId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(java.util.Map.of("error", "clientId parameter is required"));
            }
            List<Invitation> invitations = invitationService.getInvitationsByClientId(clientId);
            return ResponseEntity.ok(invitations);
        } catch (RuntimeException e) {
            System.err.println("❌ Error fetching my invitations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
