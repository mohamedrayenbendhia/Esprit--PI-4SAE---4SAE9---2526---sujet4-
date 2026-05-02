package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreatePortfolioItemRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.PortfolioItemResponse;

import java.util.List;

public interface PortfolioService {
    PortfolioItemResponse add(String orgId, CreatePortfolioItemRequest request, String userId);
    List<PortfolioItemResponse> getByOrg(String orgId);
    PortfolioItemResponse update(String itemId, String orgId, CreatePortfolioItemRequest request, String userId);
    void delete(String itemId, String orgId, String userId);
}
