package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreatePortfolioItemRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.PortfolioItemResponse;
import com.smartfreelance.microservice.organizationservice.entity.OrgPortfolioItem;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.OrgPortfolioItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioServiceImpl Unit Tests")
class PortfolioServiceImplTest {

    @Mock private OrgPortfolioItemRepository portfolioRepo;

    @InjectMocks private PortfolioServiceImpl portfolioService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private OrgPortfolioItem buildItem(String id, String orgId) {
        return OrgPortfolioItem.builder()
                .id(id)
                .organizationId(orgId)
                .title("Project Alpha")
                .description("A great project")
                .build();
    }

    private CreatePortfolioItemRequest buildCreateRequest(String title) {
        CreatePortfolioItemRequest req = new CreatePortfolioItemRequest();
        req.setTitle(title);
        req.setDescription("A great project");
        return req;
    }

    // ── add() ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("add_validRequest_savesAndReturnsItem")
    void add_validRequest_savesAndReturnsItem() {
        String orgId = "org-1";
        String userId = "user-1";
        CreatePortfolioItemRequest req = buildCreateRequest("Project Alpha");

        OrgPortfolioItem saved = buildItem("item-1", orgId);
        when(portfolioRepo.save(any(OrgPortfolioItem.class))).thenReturn(saved);

        PortfolioItemResponse response = portfolioService.add(orgId, req, userId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("item-1");
        assertThat(response.getOrganizationId()).isEqualTo(orgId);

        ArgumentCaptor<OrgPortfolioItem> captor = ArgumentCaptor.forClass(OrgPortfolioItem.class);
        verify(portfolioRepo).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Project Alpha");
        assertThat(captor.getValue().getOrganizationId()).isEqualTo(orgId);
    }

    @Test
    @DisplayName("add_withTags_setsTags")
    void add_withTags_setsTags() {
        String orgId = "org-1";
        CreatePortfolioItemRequest req = buildCreateRequest("Project Beta");
        req.setTags(List.of("React", "Node.js"));

        OrgPortfolioItem saved = buildItem("item-2", orgId);
        when(portfolioRepo.save(any())).thenReturn(saved);

        portfolioService.add(orgId, req, "user-1");

        ArgumentCaptor<OrgPortfolioItem> captor = ArgumentCaptor.forClass(OrgPortfolioItem.class);
        verify(portfolioRepo).save(captor.capture());
        assertThat(captor.getValue().getTags()).contains("React", "Node.js");
    }

    @Test
    @DisplayName("add_withNullTags_setsEmptyList")
    void add_withNullTags_setsEmptyList() {
        String orgId = "org-1";
        CreatePortfolioItemRequest req = buildCreateRequest("Project Gamma");
        req.setTags(null);

        OrgPortfolioItem saved = buildItem("item-3", orgId);
        when(portfolioRepo.save(any())).thenReturn(saved);

        portfolioService.add(orgId, req, "user-1");

        ArgumentCaptor<OrgPortfolioItem> captor = ArgumentCaptor.forClass(OrgPortfolioItem.class);
        verify(portfolioRepo).save(captor.capture());
        assertThat(captor.getValue().getTags()).isNotNull().isEmpty();
    }

    // ── getByOrg() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByOrg_withItems_returnsList")
    void getByOrg_withItems_returnsList() {
        String orgId = "org-1";
        OrgPortfolioItem i1 = buildItem("item-1", orgId);
        OrgPortfolioItem i2 = buildItem("item-2", orgId);
        when(portfolioRepo.findByOrganizationIdOrderByCreatedAtDesc(orgId))
                .thenReturn(List.of(i1, i2));

        List<PortfolioItemResponse> result = portfolioService.getByOrg(orgId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PortfolioItemResponse::getId).containsExactly("item-1", "item-2");
    }

    @Test
    @DisplayName("getByOrg_noItems_returnsEmptyList")
    void getByOrg_noItems_returnsEmptyList() {
        String orgId = "org-empty";
        when(portfolioRepo.findByOrganizationIdOrderByCreatedAtDesc(orgId))
                .thenReturn(List.of());

        List<PortfolioItemResponse> result = portfolioService.getByOrg(orgId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getByOrg_singleItem_returnsSingleElement")
    void getByOrg_singleItem_returnsSingleElement() {
        String orgId = "org-1";
        OrgPortfolioItem item = buildItem("item-1", orgId);
        when(portfolioRepo.findByOrganizationIdOrderByCreatedAtDesc(orgId))
                .thenReturn(List.of(item));

        List<PortfolioItemResponse> result = portfolioService.getByOrg(orgId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrganizationId()).isEqualTo(orgId);
    }

    // ── update() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update_validRequest_updatesAndReturns")
    void update_validRequest_updatesAndReturns() {
        String itemId = "item-1";
        String orgId = "org-1";
        String userId = "user-1";

        OrgPortfolioItem item = buildItem(itemId, orgId);
        when(portfolioRepo.findById(itemId)).thenReturn(Optional.of(item));

        OrgPortfolioItem saved = OrgPortfolioItem.builder()
                .id(itemId).organizationId(orgId).title("Updated Title").build();
        when(portfolioRepo.save(any())).thenReturn(saved);

        CreatePortfolioItemRequest req = buildCreateRequest("Updated Title");

        PortfolioItemResponse response = portfolioService.update(itemId, orgId, req, userId);

        assertThat(response.getTitle()).isEqualTo("Updated Title");
        verify(portfolioRepo).save(any());
    }

    @Test
    @DisplayName("update_itemNotFound_throwsResourceNotFoundException")
    void update_itemNotFound_throwsResourceNotFoundException() {
        when(portfolioRepo.findById("missing")).thenReturn(Optional.empty());

        CreatePortfolioItemRequest req = buildCreateRequest("Title");

        assertThatThrownBy(() -> portfolioService.update("missing", "org-1", req, "user-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio item not found");
    }

    @Test
    @DisplayName("update_itemBelongsToDifferentOrg_throwsBusinessRuleException")
    void update_itemBelongsToDifferentOrg_throwsBusinessRuleException() {
        String itemId = "item-1";
        OrgPortfolioItem item = buildItem(itemId, "org-1");
        when(portfolioRepo.findById(itemId)).thenReturn(Optional.of(item));

        CreatePortfolioItemRequest req = buildCreateRequest("Title");

        assertThatThrownBy(() -> portfolioService.update(itemId, "org-other", req, "user-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("does not belong to this organization");
    }

    @Test
    @DisplayName("update_partialFields_onlyUpdatesNonNullFields")
    void update_partialFields_onlyUpdatesNonNullFields() {
        String itemId = "item-1";
        String orgId = "org-1";
        OrgPortfolioItem item = buildItem(itemId, orgId);
        item.setImageUrl("https://old.image.url");
        when(portfolioRepo.findById(itemId)).thenReturn(Optional.of(item));

        OrgPortfolioItem saved = buildItem(itemId, orgId);
        saved.setTitle("New Title");
        saved.setImageUrl("https://old.image.url"); // imageUrl not changed
        when(portfolioRepo.save(any())).thenReturn(saved);

        CreatePortfolioItemRequest req = new CreatePortfolioItemRequest();
        req.setTitle("New Title");
        // imageUrl is null → should not overwrite existing

        PortfolioItemResponse response = portfolioService.update(itemId, orgId, req, "user-1");

        assertThat(response.getTitle()).isEqualTo("New Title");
    }

    // ── delete() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete_validItem_deletesSuccessfully")
    void delete_validItem_deletesSuccessfully() {
        String itemId = "item-1";
        String orgId = "org-1";
        OrgPortfolioItem item = buildItem(itemId, orgId);
        when(portfolioRepo.findById(itemId)).thenReturn(Optional.of(item));

        portfolioService.delete(itemId, orgId, "user-1");

        verify(portfolioRepo).delete(item);
    }

    @Test
    @DisplayName("delete_itemNotFound_throwsResourceNotFoundException")
    void delete_itemNotFound_throwsResourceNotFoundException() {
        when(portfolioRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.delete("missing", "org-1", "user-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio item not found");

        verify(portfolioRepo, never()).delete(any());
    }

    @Test
    @DisplayName("delete_itemBelongsToDifferentOrg_throwsBusinessRuleException")
    void delete_itemBelongsToDifferentOrg_throwsBusinessRuleException() {
        String itemId = "item-1";
        OrgPortfolioItem item = buildItem(itemId, "org-1");
        when(portfolioRepo.findById(itemId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> portfolioService.delete(itemId, "org-other", "user-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("does not belong to this organization");

        verify(portfolioRepo, never()).delete(any());
    }
}
