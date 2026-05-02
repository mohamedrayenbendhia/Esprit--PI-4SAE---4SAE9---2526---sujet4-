package com.microservice.module_competence.services;

import com.microservice.module_competence.entities.Endorsement;
import com.microservice.module_competence.entities.Skill;
import com.microservice.module_competence.exceptions.DuplicateResourceException;
import com.microservice.module_competence.repositories.EndorsementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndorsementServiceTest {

    @Mock EndorsementRepository endorsementRepository;
    @Mock SkillService skillService;
    @InjectMocks EndorsementService endorsementService;

    private static final String CLIENT = "client-1";
    private static final String FREELANCER = "freelancer-1";
    private static final Long SKILL_ID = 10L;

    private Skill skill() { Skill s = new Skill(); s.setId(SKILL_ID); s.setName("Java"); return s; }

    // ── endorse ───────────────────────────────────────────────────────────────

    @Test
    void endorse_success_returnsCount() {
        when(endorsementRepository.existsByClientIdAndFreelancerIdAndSkill_Id(CLIENT, FREELANCER, SKILL_ID)).thenReturn(false);
        when(skillService.findById(SKILL_ID)).thenReturn(skill());
        when(endorsementRepository.countByFreelancerIdAndSkill_Id(FREELANCER, SKILL_ID)).thenReturn(3L);

        long count = endorsementService.endorse(CLIENT, FREELANCER, SKILL_ID);

        assertThat(count).isEqualTo(3L);
        verify(endorsementRepository).save(any(Endorsement.class));
    }

    @Test
    void endorse_sameUser_throws() {
        assertThatThrownBy(() -> endorsementService.endorse(CLIENT, CLIENT, SKILL_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot endorse your own skill");
    }

    @Test
    void endorse_alreadyEndorsed_throws() {
        when(endorsementRepository.existsByClientIdAndFreelancerIdAndSkill_Id(CLIENT, FREELANCER, SKILL_ID)).thenReturn(true);

        assertThatThrownBy(() -> endorsementService.endorse(CLIENT, FREELANCER, SKILL_ID))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ── removeEndorsement ─────────────────────────────────────────────────────

    @Test
    void removeEndorsement_returnsUpdatedCount() {
        when(endorsementRepository.countByFreelancerIdAndSkill_Id(FREELANCER, SKILL_ID)).thenReturn(2L);

        long count = endorsementService.removeEndorsement(CLIENT, FREELANCER, SKILL_ID);

        verify(endorsementRepository).deleteByClientIdAndFreelancerIdAndSkill_Id(CLIENT, FREELANCER, SKILL_ID);
        assertThat(count).isEqualTo(2L);
    }

    // ── getEndorsementCount ───────────────────────────────────────────────────

    @Test
    void getEndorsementCount_returnsValue() {
        when(endorsementRepository.countByFreelancerIdAndSkill_Id(FREELANCER, SKILL_ID)).thenReturn(5L);

        assertThat(endorsementService.getEndorsementCount(FREELANCER, SKILL_ID)).isEqualTo(5L);
    }

    // ── hasEndorsed ───────────────────────────────────────────────────────────

    @Test
    void hasEndorsed_true() {
        when(endorsementRepository.existsByClientIdAndFreelancerIdAndSkill_Id(CLIENT, FREELANCER, SKILL_ID)).thenReturn(true);

        assertThat(endorsementService.hasEndorsed(CLIENT, FREELANCER, SKILL_ID)).isTrue();
    }

    @Test
    void hasEndorsed_false() {
        when(endorsementRepository.existsByClientIdAndFreelancerIdAndSkill_Id(CLIENT, FREELANCER, SKILL_ID)).thenReturn(false);

        assertThat(endorsementService.hasEndorsed(CLIENT, FREELANCER, SKILL_ID)).isFalse();
    }
}
