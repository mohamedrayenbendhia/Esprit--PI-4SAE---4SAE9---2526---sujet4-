package com.smartfreelance.microservice.organizationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreelance.microservice.organizationservice.dto.response.BadgeResponse;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.TrustBadge;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BadgeServiceImpl Unit Tests")
class BadgeServiceImplTest {

    @Mock private OrganizationRepository orgRepo;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private BadgeServiceImpl badgeService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Organization buildOrg(String orgId, String badgesJson) {
        return Organization.builder()
                .id(orgId)
                .name("Acme Corp")
                .ownerId("owner-1")
                .badges(badgesJson)
                .build();
    }

    // ── getBadges() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getBadges_orgWithBadges_returnsBadgeList")
    void getBadges_orgWithBadges_returnsBadgeList() throws Exception {
        String orgId = "org-1";
        String badgesJson = "[\"VERIFIED\",\"TOP_RATED\"]";
        Organization org = buildOrg(orgId, badgesJson);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        BadgeResponse response = badgeService.getBadges(orgId);

        assertThat(response).isNotNull();
        assertThat(response.getOrganizationId()).isEqualTo(orgId);
        assertThat(response.getBadges()).containsExactlyInAnyOrder(TrustBadge.VERIFIED, TrustBadge.TOP_RATED);
    }

    @Test
    @DisplayName("getBadges_orgWithNoBadges_returnsEmptyList")
    void getBadges_orgWithNoBadges_returnsEmptyList() {
        String orgId = "org-1";
        Organization org = buildOrg(orgId, null);
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));

        BadgeResponse response = badgeService.getBadges(orgId);

        assertThat(response.getBadges()).isEmpty();
    }

    @Test
    @DisplayName("getBadges_orgNotFound_throwsResourceNotFoundException")
    void getBadges_orgNotFound_throwsResourceNotFoundException() {
        when(orgRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> badgeService.getBadges("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Organization not found");
    }

    // ── addBadge() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("addBadge_newBadge_addsBadgeAndSaves")
    void addBadge_newBadge_addsBadgeAndSaves() throws Exception {
        String orgId = "org-1";
        Organization org = buildOrg(orgId, "[]");
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenReturn(org);

        BadgeResponse response = badgeService.addBadge(orgId, TrustBadge.VERIFIED, "admin-1");

        assertThat(response.getBadges()).contains(TrustBadge.VERIFIED);
        verify(orgRepo).save(any(Organization.class));
    }

    @Test
    @DisplayName("addBadge_duplicateBadge_doesNotAddDuplicate")
    void addBadge_duplicateBadge_doesNotAddDuplicate() {
        String orgId = "org-1";
        Organization org = buildOrg(orgId, "[\"VERIFIED\"]");
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenReturn(org);

        BadgeResponse response = badgeService.addBadge(orgId, TrustBadge.VERIFIED, "admin-1");

        // Should still contain VERIFIED but only once
        assertThat(response.getBadges()).containsOnlyOnce(TrustBadge.VERIFIED);
        verify(orgRepo).save(any(Organization.class));
    }

    @Test
    @DisplayName("addBadge_orgNotFound_throwsResourceNotFoundException")
    void addBadge_orgNotFound_throwsResourceNotFoundException() {
        when(orgRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> badgeService.addBadge("missing", TrustBadge.VERIFIED, "admin-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Organization not found");
    }

    @Test
    @DisplayName("addBadge_multipleBadges_savesAll")
    void addBadge_multipleBadges_savesAll() {
        String orgId = "org-1";
        Organization org = buildOrg(orgId, "[\"VERIFIED\"]");
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenReturn(org);

        BadgeResponse response = badgeService.addBadge(orgId, TrustBadge.TOP_RATED, "admin-1");

        assertThat(response.getBadges()).contains(TrustBadge.VERIFIED, TrustBadge.TOP_RATED);

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(orgRepo).save(captor.capture());
        assertThat(captor.getValue().getBadges()).contains("VERIFIED").contains("TOP_RATED");
    }

    // ── removeBadge() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("removeBadge_existingBadge_removesBadgeAndSaves")
    void removeBadge_existingBadge_removesBadgeAndSaves() {
        String orgId = "org-1";
        Organization org = buildOrg(orgId, "[\"VERIFIED\",\"TOP_RATED\"]");
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenReturn(org);

        BadgeResponse response = badgeService.removeBadge(orgId, TrustBadge.VERIFIED, "admin-1");

        assertThat(response.getBadges()).doesNotContain(TrustBadge.VERIFIED);
        assertThat(response.getBadges()).contains(TrustBadge.TOP_RATED);
        verify(orgRepo).save(any(Organization.class));
    }

    @Test
    @DisplayName("removeBadge_nonExistentBadge_isIdempotent")
    void removeBadge_nonExistentBadge_isIdempotent() {
        String orgId = "org-1";
        Organization org = buildOrg(orgId, "[\"TOP_RATED\"]");
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(orgRepo.save(any())).thenReturn(org);

        BadgeResponse response = badgeService.removeBadge(orgId, TrustBadge.VERIFIED, "admin-1");

        // VERIFIED was not present; list should still contain TOP_RATED only
        assertThat(response.getBadges()).doesNotContain(TrustBadge.VERIFIED);
        assertThat(response.getBadges()).contains(TrustBadge.TOP_RATED);
    }

    @Test
    @DisplayName("removeBadge_orgNotFound_throwsResourceNotFoundException")
    void removeBadge_orgNotFound_throwsResourceNotFoundException() {
        when(orgRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> badgeService.removeBadge("missing", TrustBadge.VERIFIED, "admin-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Organization not found");
    }
}
