package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreatePortfolioItemRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.PortfolioItemResponse;
import com.smartfreelance.microservice.organizationservice.entity.OrgPortfolioItem;
import com.smartfreelance.microservice.organizationservice.exception.BusinessRuleException;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.OrgPortfolioItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PortfolioServiceImpl implements PortfolioService {

    private final OrgPortfolioItemRepository portfolioRepo;

    @Override
    public PortfolioItemResponse add(String orgId, CreatePortfolioItemRequest request, String userId) {
        OrgPortfolioItem item = OrgPortfolioItem.builder()
                .organizationId(orgId).title(request.getTitle()).description(request.getDescription())
                .imageUrl(request.getImageUrl()).projectUrl(request.getProjectUrl())
                .clientName(request.getClientName())
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .completedAt(request.getCompletedAt()).build();
        return toResponse(portfolioRepo.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioItemResponse> getByOrg(String orgId) {
        return portfolioRepo.findByOrganizationIdOrderByCreatedAtDesc(orgId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public PortfolioItemResponse update(String itemId, String orgId, CreatePortfolioItemRequest request, String userId) {
        OrgPortfolioItem item = portfolioRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item not found: " + itemId));
        if (!item.getOrganizationId().equals(orgId)) {
            throw new BusinessRuleException("Portfolio item does not belong to this organization.");
        }
        if (request.getTitle() != null) item.setTitle(request.getTitle());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getImageUrl() != null) item.setImageUrl(request.getImageUrl());
        if (request.getProjectUrl() != null) item.setProjectUrl(request.getProjectUrl());
        if (request.getClientName() != null) item.setClientName(request.getClientName());
        if (request.getTags() != null) item.setTags(request.getTags());
        if (request.getCompletedAt() != null) item.setCompletedAt(request.getCompletedAt());
        return toResponse(portfolioRepo.save(item));
    }

    @Override
    public void delete(String itemId, String orgId, String userId) {
        OrgPortfolioItem item = portfolioRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item not found: " + itemId));
        if (!item.getOrganizationId().equals(orgId)) {
            throw new BusinessRuleException("Portfolio item does not belong to this organization.");
        }
        portfolioRepo.delete(item);
    }

    private PortfolioItemResponse toResponse(OrgPortfolioItem item) {
        return PortfolioItemResponse.builder()
                .id(item.getId()).organizationId(item.getOrganizationId()).title(item.getTitle())
                .description(item.getDescription()).imageUrl(item.getImageUrl())
                .projectUrl(item.getProjectUrl()).clientName(item.getClientName())
                .tags(item.getTags()).completedAt(item.getCompletedAt()).createdAt(item.getCreatedAt()).build();
    }
}
