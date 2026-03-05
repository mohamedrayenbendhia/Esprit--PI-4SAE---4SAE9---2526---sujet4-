package tn.esprit.pi.nexlance.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.pi.nexlance.entities.FreelanceProfile;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FreelanceProfileRepository extends JpaRepository<FreelanceProfile, UUID> {
    
    Optional<FreelanceProfile> findByUserId(UUID userId);
    
    Page<FreelanceProfile> findByAvailability(FreelanceProfile.Availability availability, Pageable pageable);
    
    Page<FreelanceProfile> findByLocation(String location, Pageable pageable);
    
    boolean existsByUserId(UUID userId);
}
