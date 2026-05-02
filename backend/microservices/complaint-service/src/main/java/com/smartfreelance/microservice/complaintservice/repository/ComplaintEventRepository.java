package com.smartfreelance.microservice.complaintservice.repository;

import com.smartfreelance.microservice.complaintservice.entity.ComplaintEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintEventRepository extends JpaRepository<ComplaintEvent, String> {

    /** Tous les événements d'une réclamation, triés du plus ancien au plus récent. */
    List<ComplaintEvent> findByComplaintIdOrderByOccurredAtAsc(String complaintId);

    /** Nombre d'événements pour une réclamation (utile pour les stats). */
    long countByComplaintId(String complaintId);
}
