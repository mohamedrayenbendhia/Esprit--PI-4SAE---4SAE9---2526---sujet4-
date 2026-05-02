package com.smartfreelance.microservice.organizationservice.repository;

import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.entity.OrganizationMember;
import com.smartfreelance.microservice.organizationservice.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.config.import=optional:configserver:",
        "spring.cloud.config.enabled=false",
        "spring.flyway.enabled=false"
})
@DisplayName("OrganizationMemberRepository Integration Tests")
class OrganizationMemberRepositoryTest {

    @Autowired
    private OrganizationMemberRepository memberRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private String orgId;

    @BeforeEach
    void setUp() {
        // Persist a parent organization so the org ID exists
        Organization org = Organization.builder()
                .name("Test Org Member")
                .ownerId("owner-1")
                .status(OrganizationStatus.ACTIVE)
                .type(OrganizationType.AGENCY)
                .visibility(OrganizationVisibility.PUBLIC)
                .size(OrganizationSize.SMALL)
                .build();
        Organization saved = organizationRepository.save(org);
        orgId = saved.getId();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private OrganizationMember buildMember(String userId, MemberRole role, MemberStatus status) {
        return OrganizationMember.builder()
                .organizationId(orgId)
                .userId(userId)
                .role(role)
                .status(status)
                .build();
    }

    // ── findByOrganizationIdAndUserId() ───────────────────────────────────────

    @Test
    @DisplayName("findByOrganizationIdAndUserId_existingMember_returnsMember")
    void findByOrganizationIdAndUserId_existingMember_returnsMember() {
        OrganizationMember member = buildMember("user-1", MemberRole.MEMBER, MemberStatus.ACTIVE);
        memberRepository.save(member);

        Optional<OrganizationMember> found = memberRepository.findByOrganizationIdAndUserId(orgId, "user-1");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("user-1");
        assertThat(found.get().getRole()).isEqualTo(MemberRole.MEMBER);
    }

    @Test
    @DisplayName("findByOrganizationIdAndUserId_nonExistingMember_returnsEmpty")
    void findByOrganizationIdAndUserId_nonExistingMember_returnsEmpty() {
        Optional<OrganizationMember> found = memberRepository.findByOrganizationIdAndUserId(orgId, "ghost-user");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByOrganizationIdAndUserId_wrongOrgId_returnsEmpty")
    void findByOrganizationIdAndUserId_wrongOrgId_returnsEmpty() {
        OrganizationMember member = buildMember("user-2", MemberRole.MANAGER, MemberStatus.ACTIVE);
        memberRepository.save(member);

        Optional<OrganizationMember> found = memberRepository.findByOrganizationIdAndUserId("wrong-org", "user-2");

        assertThat(found).isEmpty();
    }

    // ── findByOrganizationIdAndStatus() ──────────────────────────────────────

    @Test
    @DisplayName("findByOrganizationIdAndStatus_active_returnsOnlyActiveMembers")
    void findByOrganizationIdAndStatus_active_returnsOnlyActiveMembers() {
        OrganizationMember active1 = buildMember("user-a1", MemberRole.MEMBER, MemberStatus.ACTIVE);
        OrganizationMember active2 = buildMember("user-a2", MemberRole.MANAGER, MemberStatus.ACTIVE);
        OrganizationMember inactive = buildMember("user-a3", MemberRole.MEMBER, MemberStatus.INACTIVE);
        memberRepository.saveAll(List.of(active1, active2, inactive));

        List<OrganizationMember> results = memberRepository.findByOrganizationIdAndStatus(orgId, MemberStatus.ACTIVE);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(OrganizationMember::getStatus)
                .containsOnly(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByOrganizationIdAndStatus_noMatching_returnsEmptyList")
    void findByOrganizationIdAndStatus_noMatching_returnsEmptyList() {
        OrganizationMember active = buildMember("user-b1", MemberRole.MEMBER, MemberStatus.ACTIVE);
        memberRepository.save(active);

        List<OrganizationMember> results = memberRepository.findByOrganizationIdAndStatus(orgId, MemberStatus.INACTIVE);

        assertThat(results).isEmpty();
    }

    // ── findByOrganizationId() (pageable) ─────────────────────────────────────

    @Test
    @DisplayName("findByOrganizationId_multipleMembers_returnsPage")
    void findByOrganizationId_multipleMembers_returnsPage() {
        OrganizationMember m1 = buildMember("user-c1", MemberRole.MEMBER, MemberStatus.ACTIVE);
        OrganizationMember m2 = buildMember("user-c2", MemberRole.MANAGER, MemberStatus.ACTIVE);
        OrganizationMember m3 = buildMember("user-c3", MemberRole.MEMBER, MemberStatus.INACTIVE);
        memberRepository.saveAll(List.of(m1, m2, m3));

        Page<OrganizationMember> page = memberRepository.findByOrganizationId(orgId, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("findByOrganizationId_pagination_respectsPageSize")
    void findByOrganizationId_pagination_respectsPageSize() {
        for (int i = 0; i < 5; i++) {
            memberRepository.save(buildMember("user-page-" + i, MemberRole.MEMBER, MemberStatus.ACTIVE));
        }

        Page<OrganizationMember> page = memberRepository.findByOrganizationId(orgId, PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
    }

    // ── findByUserId() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUserId_memberInMultipleOrgs_returnsAll")
    void findByUserId_memberInMultipleOrgs_returnsAll() {
        // Create a second organization
        Organization org2 = Organization.builder()
                .name("Second Org Member")
                .ownerId("owner-2")
                .status(OrganizationStatus.ACTIVE)
                .type(OrganizationType.STARTUP)
                .visibility(OrganizationVisibility.PUBLIC)
                .size(OrganizationSize.MEDIUM)
                .build();
        Organization saved2 = organizationRepository.save(org2);

        OrganizationMember m1 = OrganizationMember.builder()
                .organizationId(orgId).userId("shared-user").role(MemberRole.MEMBER)
                .status(MemberStatus.ACTIVE).build();
        OrganizationMember m2 = OrganizationMember.builder()
                .organizationId(saved2.getId()).userId("shared-user").role(MemberRole.MANAGER)
                .status(MemberStatus.ACTIVE).build();
        memberRepository.saveAll(List.of(m1, m2));

        List<OrganizationMember> results = memberRepository.findByUserId("shared-user");

        assertThat(results).hasSize(2);
        assertThat(results).extracting(OrganizationMember::getUserId).containsOnly("shared-user");
    }

    @Test
    @DisplayName("findByUserId_noMembership_returnsEmptyList")
    void findByUserId_noMembership_returnsEmptyList() {
        List<OrganizationMember> results = memberRepository.findByUserId("never-joined");

        assertThat(results).isEmpty();
    }

    // ── existsByOrganizationIdAndUserIdAndStatus() ────────────────────────────

    @Test
    @DisplayName("existsByOrganizationIdAndUserIdAndStatus_activeMember_returnsTrue")
    void existsByOrganizationIdAndUserIdAndStatus_activeMember_returnsTrue() {
        OrganizationMember member = buildMember("user-ex1", MemberRole.MEMBER, MemberStatus.ACTIVE);
        memberRepository.save(member);

        boolean exists = memberRepository.existsByOrganizationIdAndUserIdAndStatus(
                orgId, "user-ex1", MemberStatus.ACTIVE);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByOrganizationIdAndUserIdAndStatus_inactiveMember_returnsFalse")
    void existsByOrganizationIdAndUserIdAndStatus_inactiveMember_returnsFalse() {
        OrganizationMember member = buildMember("user-ex2", MemberRole.MEMBER, MemberStatus.INACTIVE);
        memberRepository.save(member);

        boolean exists = memberRepository.existsByOrganizationIdAndUserIdAndStatus(
                orgId, "user-ex2", MemberStatus.ACTIVE);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByOrganizationIdAndUserIdAndStatus_nonExistingMember_returnsFalse")
    void existsByOrganizationIdAndUserIdAndStatus_nonExistingMember_returnsFalse() {
        boolean exists = memberRepository.existsByOrganizationIdAndUserIdAndStatus(
                orgId, "ghost-user", MemberStatus.ACTIVE);

        assertThat(exists).isFalse();
    }

    // ── countByOrganizationIdAndStatus() ─────────────────────────────────────

    @Test
    @DisplayName("countByOrganizationIdAndStatus_correctCount")
    void countByOrganizationIdAndStatus_correctCount() {
        OrganizationMember a1 = buildMember("user-cnt1", MemberRole.MEMBER, MemberStatus.ACTIVE);
        OrganizationMember a2 = buildMember("user-cnt2", MemberRole.MANAGER, MemberStatus.ACTIVE);
        OrganizationMember i1 = buildMember("user-cnt3", MemberRole.MEMBER, MemberStatus.INACTIVE);
        memberRepository.saveAll(List.of(a1, a2, i1));

        long activeCount = memberRepository.countByOrganizationIdAndStatus(orgId, MemberStatus.ACTIVE);
        long inactiveCount = memberRepository.countByOrganizationIdAndStatus(orgId, MemberStatus.INACTIVE);

        assertThat(activeCount).isEqualTo(2);
        assertThat(inactiveCount).isEqualTo(1);
    }

    @Test
    @DisplayName("countByOrganizationIdAndStatus_emptyOrg_returnsZero")
    void countByOrganizationIdAndStatus_emptyOrg_returnsZero() {
        long count = memberRepository.countByOrganizationIdAndStatus("empty-org", MemberStatus.ACTIVE);

        assertThat(count).isEqualTo(0);
    }

    // ── findByOrganizationIdAndRole() ─────────────────────────────────────────

    @Test
    @DisplayName("findByOrganizationIdAndRole_managers_returnsOnlyManagers")
    void findByOrganizationIdAndRole_managers_returnsOnlyManagers() {
        OrganizationMember owner = buildMember("user-r1", MemberRole.OWNER, MemberStatus.ACTIVE);
        OrganizationMember manager = buildMember("user-r2", MemberRole.MANAGER, MemberStatus.ACTIVE);
        OrganizationMember member = buildMember("user-r3", MemberRole.MEMBER, MemberStatus.ACTIVE);
        memberRepository.saveAll(List.of(owner, manager, member));

        List<OrganizationMember> managers = memberRepository.findByOrganizationIdAndRole(orgId, MemberRole.MANAGER);

        assertThat(managers).hasSize(1);
        assertThat(managers.get(0).getRole()).isEqualTo(MemberRole.MANAGER);
        assertThat(managers.get(0).getUserId()).isEqualTo("user-r2");
    }

    @Test
    @DisplayName("findByOrganizationIdAndRole_noMatchingRole_returnsEmptyList")
    void findByOrganizationIdAndRole_noMatchingRole_returnsEmptyList() {
        OrganizationMember member = buildMember("user-r4", MemberRole.MEMBER, MemberStatus.ACTIVE);
        memberRepository.save(member);

        List<OrganizationMember> owners = memberRepository.findByOrganizationIdAndRole(orgId, MemberRole.OWNER);

        assertThat(owners).isEmpty();
    }

    @Test
    @DisplayName("findByOrganizationIdAndRole_multipleOwners_returnsAll")
    void findByOrganizationIdAndRole_multipleOwners_returnsAll() {
        OrganizationMember o1 = buildMember("user-r5", MemberRole.OWNER, MemberStatus.ACTIVE);
        OrganizationMember o2 = buildMember("user-r6", MemberRole.OWNER, MemberStatus.INACTIVE);
        memberRepository.saveAll(List.of(o1, o2));

        List<OrganizationMember> owners = memberRepository.findByOrganizationIdAndRole(orgId, MemberRole.OWNER);

        assertThat(owners).hasSize(2);
        assertThat(owners).extracting(OrganizationMember::getRole).containsOnly(MemberRole.OWNER);
    }
}
