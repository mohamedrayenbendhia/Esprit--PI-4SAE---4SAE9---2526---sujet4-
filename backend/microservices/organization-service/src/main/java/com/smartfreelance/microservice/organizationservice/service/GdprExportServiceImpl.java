package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class GdprExportServiceImpl implements GdprExportService {

    private final OrganizationRepository orgRepo;
    private final OrganizationMemberRepository memberRepo;
    private final OrgApplicationRepository appRepo;
    private final AuditLogRepository auditRepo;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> exportUserData(String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("ownedOrganizations", orgRepo.findByOwnerId(userId));
        data.put("memberships", memberRepo.findByUserId(userId));
        data.put("applications", appRepo.findByApplicantId(userId));
        data.put("auditLogs", auditRepo.findByPerformedByUserId(userId));
        return data;
    }

    @Override
    public void deleteUserData(String userId) {
        memberRepo.findByUserId(userId).forEach(m -> {
            m.setUserId("DELETED");
            memberRepo.save(m);
        });
        appRepo.findByApplicantId(userId).forEach(a -> {
            a.setApplicantId("DELETED");
            appRepo.save(a);
        });
    }
}
