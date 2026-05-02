package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.request.CreateRfqRequest;
import com.smartfreelance.microservice.organizationservice.dto.request.RfqResponseRequest;
import com.smartfreelance.microservice.organizationservice.dto.response.RfqResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RfqService {
    RfqResponse create(String orgId, CreateRfqRequest request, String requesterId);
    Page<RfqResponse> getByOrg(String orgId, Pageable pageable);
    RfqResponse respond(String rfqId, RfqResponseRequest request, String responderId);
    void close(String rfqId, String userId);
    List<RfqResponse> getMyRfqs(String requesterId);
}
