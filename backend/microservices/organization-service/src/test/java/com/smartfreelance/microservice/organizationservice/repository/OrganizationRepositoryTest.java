package com.smartfreelance.microservice.organizationservice.repository;

import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationSize;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationStatus;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationType;
import com.smartfreelance.microservice.organizationservice.enums.OrganizationVisibility;
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
@DisplayName("OrganizationRepository Integration Tests")
class OrganizationRepositoryTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Organization buildOrg(String name, String ownerId, OrganizationStatus status,
                                   OrganizationType type, OrganizationVisibility visibility) {
        return Organization.builder()
                .name(name)
                .ownerId(ownerId)
                .status(status)
                .type(type)
                .visibility(visibility)
                .size(OrganizationSize.SMALL)
                .build();
    }

    // ── findByName() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByName_existingName_returnsOrganization")
    void findByName_existingName_returnsOrganization() {
        Organization org = buildOrg("Acme Corp", "owner-1", OrganizationStatus.ACTIVE,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        organizationRepository.save(org);

        Optional<Organization> found = organizationRepository.findByName("Acme Corp");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Acme Corp");
        assertThat(found.get().getOwnerId()).isEqualTo("owner-1");
    }

    @Test
    @DisplayName("findByName_nonExistingName_returnsEmpty")
    void findByName_nonExistingName_returnsEmpty() {
        Optional<Organization> found = organizationRepository.findByName("Unknown Corp");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByName_uniqueConstraint_onlyOneResult")
    void findByName_uniqueConstraint_onlyOneResult() {
        Organization o1 = buildOrg("Unique Corp", "owner-1", OrganizationStatus.ACTIVE,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        organizationRepository.save(o1);

        Optional<Organization> found = organizationRepository.findByName("Unique Corp");

        assertThat(found).isPresent();
    }

    // ── findByOwnerId() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("findByOwnerId_multipleOrgs_returnsAll")
    void findByOwnerId_multipleOrgs_returnsAll() {
        Organization o1 = buildOrg("Corp A", "owner-99", OrganizationStatus.ACTIVE,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        Organization o2 = buildOrg("Corp B", "owner-99", OrganizationStatus.SUSPENDED,
                OrganizationType.STARTUP, OrganizationVisibility.PRIVATE);
        organizationRepository.saveAll(List.of(o1, o2));

        List<Organization> results = organizationRepository.findByOwnerId("owner-99");

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Organization::getOwnerId).containsOnly("owner-99");
    }

    @Test
    @DisplayName("findByOwnerId_noOrgs_returnsEmptyList")
    void findByOwnerId_noOrgs_returnsEmptyList() {
        List<Organization> results = organizationRepository.findByOwnerId("unknown-owner");

        assertThat(results).isEmpty();
    }

    // ── findByStatus() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByStatus_existingStatus_returnsPage")
    void findByStatus_existingStatus_returnsPage() {
        Organization o1 = buildOrg("Active Corp", "owner-1", OrganizationStatus.ACTIVE,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        Organization o2 = buildOrg("Another Active", "owner-2", OrganizationStatus.ACTIVE,
                OrganizationType.STARTUP, OrganizationVisibility.PUBLIC);
        Organization o3 = buildOrg("Suspended Corp", "owner-3", OrganizationStatus.SUSPENDED,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        organizationRepository.saveAll(List.of(o1, o2, o3));

        Page<Organization> page = organizationRepository.findByStatus(
                OrganizationStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Organization::getStatus)
                .containsOnly(OrganizationStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByStatus_noMatchingStatus_returnsEmptyPage")
    void findByStatus_noMatchingStatus_returnsEmptyPage() {
        Organization o1 = buildOrg("Active Corp 2", "owner-1", OrganizationStatus.ACTIVE,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        organizationRepository.save(o1);

        Page<Organization> page = organizationRepository.findByStatus(
                OrganizationStatus.DISSOLVED, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }

    // ── existsByName() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("existsByName_existingName_returnsTrue")
    void existsByName_existingName_returnsTrue() {
        Organization org = buildOrg("Exists Corp", "owner-1", OrganizationStatus.ACTIVE,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        organizationRepository.save(org);

        assertThat(organizationRepository.existsByName("Exists Corp")).isTrue();
    }

    @Test
    @DisplayName("existsByName_nonExistingName_returnsFalse")
    void existsByName_nonExistingName_returnsFalse() {
        assertThat(organizationRepository.existsByName("Ghost Corp")).isFalse();
    }

    // ── countByStatus() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("countByStatus_correctCount")
    void countByStatus_correctCount() {
        Organization o1 = buildOrg("Pending Corp 1", "owner-1", OrganizationStatus.PENDING_VERIFICATION,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        Organization o2 = buildOrg("Pending Corp 2", "owner-2", OrganizationStatus.PENDING_VERIFICATION,
                OrganizationType.STARTUP, OrganizationVisibility.PUBLIC);
        Organization o3 = buildOrg("Active Corp 3", "owner-3", OrganizationStatus.ACTIVE,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        organizationRepository.saveAll(List.of(o1, o2, o3));

        long count = organizationRepository.countByStatus(OrganizationStatus.PENDING_VERIFICATION);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByStatus_noMatch_returnsZero")
    void countByStatus_noMatch_returnsZero() {
        long count = organizationRepository.countByStatus(OrganizationStatus.DISSOLVED);

        assertThat(count).isEqualTo(0);
    }

    // ── findByStatusAndVisibility() ───────────────────────────────────────────

    @Test
    @DisplayName("findByStatusAndVisibility_matchingFilter_returnsList")
    void findByStatusAndVisibility_matchingFilter_returnsList() {
        Organization pub = buildOrg("Public Active", "owner-1", OrganizationStatus.ACTIVE,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        Organization priv = buildOrg("Private Active", "owner-2", OrganizationStatus.ACTIVE,
                OrganizationType.AGENCY, OrganizationVisibility.PRIVATE);
        organizationRepository.saveAll(List.of(pub, priv));

        List<Organization> results = organizationRepository.findByStatusAndVisibility(
                OrganizationStatus.ACTIVE, OrganizationVisibility.PUBLIC);

        assertThat(results).isNotEmpty();
        assertThat(results).extracting(Organization::getVisibility)
                .containsOnly(OrganizationVisibility.PUBLIC);
    }

    // ── searchPublic() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchPublic_keywordMatch_returnsMatchingOrgs")
    void searchPublic_keywordMatch_returnsMatchingOrgs() {
        Organization o1 = buildOrg("Design Studio", "owner-1", OrganizationStatus.ACTIVE,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        Organization o2 = buildOrg("Dev Factory", "owner-2", OrganizationStatus.ACTIVE,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        organizationRepository.saveAll(List.of(o1, o2));

        Page<Organization> results = organizationRepository.searchPublic(
                "Design", null, null, PageRequest.of(0, 10));

        assertThat(results.getContent()).isNotEmpty();
        assertThat(results.getContent()).extracting(Organization::getName)
                .anyMatch(name -> name.contains("Design"));
    }

    @Test
    @DisplayName("searchPublic_typeFilter_returnsOnlyMatchingType")
    void searchPublic_typeFilter_returnsOnlyMatchingType() {
        Organization agency = buildOrg("Agency One", "owner-1", OrganizationStatus.ACTIVE,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        Organization company = buildOrg("Company One", "owner-2", OrganizationStatus.ACTIVE,
                OrganizationType.STARTUP, OrganizationVisibility.PUBLIC);
        organizationRepository.saveAll(List.of(agency, company));

        Page<Organization> results = organizationRepository.searchPublic(
                null, OrganizationType.AGENCY, null, PageRequest.of(0, 10));

        assertThat(results.getContent()).isNotEmpty();
        assertThat(results.getContent()).extracting(Organization::getType)
                .containsOnly(OrganizationType.AGENCY);
    }

    // ── findAllNonDissolved() ─────────────────────────────────────────────────

    @Test
    @DisplayName("findAllNonDissolved_excludesDissolvedAndRejected")
    void findAllNonDissolved_excludesDissolvedAndRejected() {
        Organization active = buildOrg("Active Org ND", "owner-1", OrganizationStatus.ACTIVE,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        Organization dissolved = buildOrg("Dissolved Org ND", "owner-2", OrganizationStatus.DISSOLVED,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        Organization rejected = buildOrg("Rejected Org ND", "owner-3", OrganizationStatus.REJECTED,
                OrganizationType.AGENCY, OrganizationVisibility.PUBLIC);
        organizationRepository.saveAll(List.of(active, dissolved, rejected));

        List<Organization> results = organizationRepository.findAllNonDissolved();

        assertThat(results).extracting(Organization::getStatus)
                .doesNotContain(OrganizationStatus.DISSOLVED, OrganizationStatus.REJECTED);
        assertThat(results).extracting(Organization::getName).contains("Active Org ND");
    }
}
