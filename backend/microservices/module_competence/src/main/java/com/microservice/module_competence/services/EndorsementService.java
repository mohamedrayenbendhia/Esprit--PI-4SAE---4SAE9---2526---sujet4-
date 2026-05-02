package com.microservice.module_competence.services;

import com.microservice.module_competence.entities.*;
import com.microservice.module_competence.exceptions.*;
import com.microservice.module_competence.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EndorsementService {

    private final EndorsementRepository endorsementRepository;
    private final SkillService skillService;

    @Transactional
    public long endorse(String clientId, String freelancerId, Long skillId) {
        if (clientId.equals(freelancerId)) {
            throw new RuntimeException("Cannot endorse your own skill");
        }
        if (endorsementRepository.existsByClientIdAndFreelancerIdAndSkill_Id(clientId, freelancerId, skillId)) {
            throw new DuplicateResourceException("You already endorsed this skill");
        }
        Skill skill = skillService.findById(skillId);
        Endorsement endorsement = Endorsement.builder()
                .clientId(clientId)
                .freelancerId(freelancerId)
                .skill(skill)
                .build();
        endorsementRepository.save(endorsement);
        return endorsementRepository.countByFreelancerIdAndSkill_Id(freelancerId, skillId);
    }

    @Transactional
    public long removeEndorsement(String clientId, String freelancerId, Long skillId) {
        endorsementRepository.deleteByClientIdAndFreelancerIdAndSkill_Id(clientId, freelancerId, skillId);
        return endorsementRepository.countByFreelancerIdAndSkill_Id(freelancerId, skillId);
    }

    public long getEndorsementCount(String freelancerId, Long skillId) {
        return endorsementRepository.countByFreelancerIdAndSkill_Id(freelancerId, skillId);
    }

    public boolean hasEndorsed(String clientId, String freelancerId, Long skillId) {
        return endorsementRepository.existsByClientIdAndFreelancerIdAndSkill_Id(clientId, freelancerId, skillId);
    }
}