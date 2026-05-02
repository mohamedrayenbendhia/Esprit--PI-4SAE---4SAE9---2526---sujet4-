package tn.esprit.pi.nexlance.invitation.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.pi.nexlance.invitation.entities.Invitation;
import tn.esprit.pi.nexlance.invitation.enums.InvitationStatus;

import java.util.List;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    
    // Récupérer toutes les invitations d'un client
    List<Invitation> findByClientId(String clientId);
    
    // Récupérer toutes les invitations reçues par un freelance
    List<Invitation> findByFreelanceId(String freelanceId);
    
    // Récupérer les invitations pour une offre spécifique
    List<Invitation> findByJobOfferId(String jobOfferId);
    
    // Récupérer les invitations par statut
    List<Invitation> findByStatus(InvitationStatus status);
    
    // Récupérer les invitations d'un client avec un statut spécifique
    List<Invitation> findByClientIdAndStatus(String clientId, InvitationStatus status);
    
    // Récupérer les invitations d'un freelance avec un statut spécifique
    List<Invitation> findByFreelanceIdAndStatus(String freelanceId, InvitationStatus status);
    
    // Vérifier si une invitation existe déjà
    boolean existsByClientIdAndFreelanceIdAndJobOfferId(String clientId, String freelanceId, String jobOfferId);
}
