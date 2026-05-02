package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.response.MemberResponse;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberService {
    Page<MemberResponse> getMembers(String orgId, Pageable pageable);
    MemberResponse updateRole(String orgId, String memberId, MemberRole newRole, String requesterId);
    void removeMember(String orgId, String memberId, String requesterId);
    void leave(String orgId, String userId);
    List<MemberResponse> getMyMemberships(String userId);
    MemberResponse toggleProfileDisplay(String orgId, String userId, boolean display);
}
