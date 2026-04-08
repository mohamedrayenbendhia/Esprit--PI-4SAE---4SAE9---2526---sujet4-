package com.microservice.module_competence.repositories;

import com.microservice.module_competence.entities.Endorsement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EndorsementRepository extends JpaRepository<Endorsement, Long> {
    boolean existsByClientIdAndFreelancerIdAndSkill_Id(String clientId, String freelancerId, Long skillId);
    long countByFreelancerIdAndSkill_Id(String freelancerId, Long skillId);
    List<Endorsement> findByFreelancerIdAndSkill_Id(String freelancerId, Long skillId);
    void deleteByClientIdAndFreelancerIdAndSkill_Id(String clientId, String freelancerId, Long skillId);
}