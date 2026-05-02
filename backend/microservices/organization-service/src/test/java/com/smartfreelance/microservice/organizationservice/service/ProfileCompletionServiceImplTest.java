package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.response.ProfileCompletionResponse;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.entity.OrganizationMember;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.enums.MemberStatus;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.OrgPortfolioItemRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationMemberRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileCompletionServiceImpl Unit Tests")
class ProfileCompletionServiceImplTest {

    /* ── Mocks ───────────────────────────────────────────────────────────── */

    @Mock private OrganizationRepository       orgRepo;
    @Mock private OrganizationMemberRepository memberRepo;
    @Mock private OrgPortfolioItemRepository   portfolioRepo;

    @InjectMocks
    private ProfileCompletionServiceImpl profileCompletionService;

    /* ── Helpers ─────────────────────────────────────────────────────────── */

    /**
     * Builds a fully-completed Organization (all criteria met → score 100).
     */
    private Organization buildFullOrg(String orgId) {
        return Organization.builder()
                .id(orgId)
                .name("Full Corp")
                .description("A complete organization description.")
                .logoUrl("https://example.com/logo.png")
                .website("https://example.com")
                .specialties(List.of("Java", "Spring", "Docker"))
                .siret("12345678901234")
                .location("Paris, France")
                .build();
    }

    /**
     * Builds an Organization with no optional fields filled.
     */
    private Organization buildEmptyOrg(String orgId) {
        return Organization.builder()
                .id(orgId)
                .name("Empty Corp")
                .build();
    }

    private OrganizationMember buildActiveMember(String orgId, MemberRole role, MemberStatus status) {
        return OrganizationMember.builder()
                .id("mem-1")
                .organizationId(orgId)
                .userId("user-2")
                .role(role)
                .status(status)
                .build();
    }

    /* ── compute() ───────────────────────────────────────────────────────── */

    @Test
    @DisplayName("compute_orgNotFound_throwsResourceNotFoundException")
    void compute_orgNotFound_throwsResourceNotFoundException() {
        when(orgRepo.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileCompletionService.compute("unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    @DisplayName("compute_allCriteriaMet_returns100")
    void compute_allCriteriaMet_returns100() {
        String orgId = "org-full";
        Organization org = buildFullOrg(orgId);

        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(portfolioRepo.countByOrganizationId(orgId)).thenReturn(3L);

        // Has a MANAGER role member → hasTeamMember = true
        OrganizationMember manager = buildActiveMember(orgId, MemberRole.MANAGER, MemberStatus.ACTIVE);
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MANAGER)).thenReturn(List.of(manager));

        ProfileCompletionResponse response = profileCompletionService.compute(orgId);

        assertThat(response).isNotNull();
        assertThat(response.getScore()).isEqualTo(100);
        assertThat(response.isVisibleInSearch()).isTrue();
        assertThat(response.getOrganizationId()).isEqualTo(orgId);

        ProfileCompletionResponse.Breakdown breakdown = response.getBreakdown();
        assertThat(breakdown.isHasDescription()).isTrue();
        assertThat(breakdown.isHasLogo()).isTrue();
        assertThat(breakdown.isHasWebsite()).isTrue();
        assertThat(breakdown.isHasSpecialties()).isTrue();
        assertThat(breakdown.isHasSiret()).isTrue();
        assertThat(breakdown.isHasLocation()).isTrue();
        assertThat(breakdown.isHasPortfolio()).isTrue();
        assertThat(breakdown.isHasTeamMember()).isTrue();
        assertThat(breakdown.getMissingItems()).isEmpty();
    }

    @Test
    @DisplayName("compute_emptyCriteria_returns0")
    void compute_emptyCriteria_returns0() {
        String orgId = "org-empty";
        Organization org = buildEmptyOrg(orgId);

        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(portfolioRepo.countByOrganizationId(orgId)).thenReturn(0L);
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MANAGER))
                .thenReturn(Collections.emptyList());
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MEMBER))
                .thenReturn(Collections.emptyList());

        ProfileCompletionResponse response = profileCompletionService.compute(orgId);

        assertThat(response.getScore()).isEqualTo(0);
        assertThat(response.isVisibleInSearch()).isFalse();

        ProfileCompletionResponse.Breakdown breakdown = response.getBreakdown();
        assertThat(breakdown.isHasDescription()).isFalse();
        assertThat(breakdown.isHasLogo()).isFalse();
        assertThat(breakdown.isHasWebsite()).isFalse();
        assertThat(breakdown.isHasSpecialties()).isFalse();
        assertThat(breakdown.isHasSiret()).isFalse();
        assertThat(breakdown.isHasLocation()).isFalse();
        assertThat(breakdown.isHasPortfolio()).isFalse();
        assertThat(breakdown.isHasTeamMember()).isFalse();
        // All 8 missing items hints expected
        assertThat(breakdown.getMissingItems()).hasSize(8);
    }

    @Test
    @DisplayName("compute_partialCriteria_returnsCorrectScore")
    void compute_partialCriteria_returnsCorrectScore() {
        String orgId = "org-partial";
        // description (15) + logo (10) + location (10) = 35 expected
        Organization org = Organization.builder()
                .id(orgId)
                .name("Partial Corp")
                .description("Some description")
                .logoUrl("https://example.com/logo.png")
                .location("Lyon")
                .build();

        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(portfolioRepo.countByOrganizationId(orgId)).thenReturn(0L);
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MANAGER))
                .thenReturn(Collections.emptyList());
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MEMBER))
                .thenReturn(Collections.emptyList());

        ProfileCompletionResponse response = profileCompletionService.compute(orgId);

        assertThat(response.getScore()).isEqualTo(35); // 15 + 10 + 10
        assertThat(response.isVisibleInSearch()).isFalse(); // below 40
    }

    @Test
    @DisplayName("compute_scoreBelow40_visibleInSearchFalse")
    void compute_scoreBelow40_visibleInSearchFalse() {
        String orgId = "org-below40";
        // Only description (15) + logo (10) + website (10) = 35 < 40
        Organization org = Organization.builder()
                .id(orgId)
                .name("Below40 Corp")
                .description("A description")
                .logoUrl("https://logo.png")
                .website("https://site.com")
                .build();

        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(portfolioRepo.countByOrganizationId(orgId)).thenReturn(0L);
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MANAGER))
                .thenReturn(Collections.emptyList());
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MEMBER))
                .thenReturn(Collections.emptyList());

        ProfileCompletionResponse response = profileCompletionService.compute(orgId);

        assertThat(response.getScore()).isEqualTo(35);
        assertThat(response.isVisibleInSearch()).isFalse();
    }

    @Test
    @DisplayName("compute_scoreAbove40_visibleInSearchTrue")
    void compute_scoreAbove40_visibleInSearchTrue() {
        String orgId = "org-above40";
        // description (15) + logo (10) + website (10) + specialties (15) = 50 >= 40
        Organization org = Organization.builder()
                .id(orgId)
                .name("Above40 Corp")
                .description("A description")
                .logoUrl("https://logo.png")
                .website("https://site.com")
                .specialties(List.of("React", "Node", "AWS"))
                .build();

        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(portfolioRepo.countByOrganizationId(orgId)).thenReturn(0L);
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MANAGER))
                .thenReturn(Collections.emptyList());
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MEMBER))
                .thenReturn(Collections.emptyList());

        ProfileCompletionResponse response = profileCompletionService.compute(orgId);

        assertThat(response.getScore()).isEqualTo(50);
        assertThat(response.isVisibleInSearch()).isTrue();
    }

    @Test
    @DisplayName("compute_hasActiveMemberWithRoleMember_countsAsTeamMember")
    void compute_hasActiveMemberWithRoleMember_countsAsTeamMember() {
        String orgId = "org-member";
        Organization org = buildFullOrg(orgId);

        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(portfolioRepo.countByOrganizationId(orgId)).thenReturn(1L);

        // No MANAGER
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MANAGER))
                .thenReturn(Collections.emptyList());
        // One active MEMBER
        OrganizationMember activeMember = buildActiveMember(orgId, MemberRole.MEMBER, MemberStatus.ACTIVE);
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MEMBER))
                .thenReturn(List.of(activeMember));

        ProfileCompletionResponse response = profileCompletionService.compute(orgId);

        assertThat(response.getBreakdown().isHasTeamMember()).isTrue();
        assertThat(response.getScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("compute_inactiveMemberOnly_teamMemberFalse")
    void compute_inactiveMemberOnly_teamMemberFalse() {
        String orgId = "org-inactive-member";
        Organization org = Organization.builder()
                .id(orgId)
                .name("Inactive Member Corp")
                .build();

        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(portfolioRepo.countByOrganizationId(orgId)).thenReturn(0L);

        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MANAGER))
                .thenReturn(Collections.emptyList());
        // Only INACTIVE member
        OrganizationMember inactiveMember = buildActiveMember(orgId, MemberRole.MEMBER, MemberStatus.INACTIVE);
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MEMBER))
                .thenReturn(List.of(inactiveMember));

        ProfileCompletionResponse response = profileCompletionService.compute(orgId);

        assertThat(response.getBreakdown().isHasTeamMember()).isFalse();
    }

    /* ── computeScore() ──────────────────────────────────────────────────── */

    @Test
    @DisplayName("computeScore_delegatesToBuildResponse")
    void computeScore_delegatesToBuildResponse() {
        String orgId = "org-score";
        Organization org = buildFullOrg(orgId);

        when(portfolioRepo.countByOrganizationId(orgId)).thenReturn(2L);
        OrganizationMember manager = buildActiveMember(orgId, MemberRole.MANAGER, MemberStatus.ACTIVE);
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MANAGER)).thenReturn(List.of(manager));

        int score = profileCompletionService.computeScore(org);

        assertThat(score).isEqualTo(100);
        // orgRepo is NOT called — computeScore uses the passed entity directly
        verify(orgRepo, never()).findById(any());
    }

    @Test
    @DisplayName("computeScore_emptyOrg_returns0")
    void computeScore_emptyOrg_returns0() {
        String orgId = "org-score-zero";
        Organization org = buildEmptyOrg(orgId);

        when(portfolioRepo.countByOrganizationId(orgId)).thenReturn(0L);
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MANAGER))
                .thenReturn(Collections.emptyList());
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MEMBER))
                .thenReturn(Collections.emptyList());

        int score = profileCompletionService.computeScore(org);

        assertThat(score).isEqualTo(0);
    }

    @Test
    @DisplayName("compute_specialtiesBelowThree_hasSpecialtiesFalse")
    void compute_specialtiesBelowThree_hasSpecialtiesFalse() {
        String orgId = "org-few-specs";
        Organization org = Organization.builder()
                .id(orgId)
                .name("Few Spec Corp")
                .specialties(List.of("Java", "Spring")) // only 2, need >= 3
                .build();

        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(portfolioRepo.countByOrganizationId(orgId)).thenReturn(0L);
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MANAGER))
                .thenReturn(Collections.emptyList());
        when(memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MEMBER))
                .thenReturn(Collections.emptyList());

        ProfileCompletionResponse response = profileCompletionService.compute(orgId);

        assertThat(response.getBreakdown().isHasSpecialties()).isFalse();
    }
}
