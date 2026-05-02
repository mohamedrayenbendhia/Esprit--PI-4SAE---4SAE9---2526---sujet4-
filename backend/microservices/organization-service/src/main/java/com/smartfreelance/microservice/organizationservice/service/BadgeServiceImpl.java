package com.smartfreelance.microservice.organizationservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreelance.microservice.organizationservice.dto.response.BadgeResponse;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.TrustBadge;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BadgeServiceImpl implements BadgeService {

    private final OrganizationRepository orgRepo;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public BadgeResponse getBadges(String orgId) {
        Organization org = findOrThrow(orgId);
        return BadgeResponse.builder().organizationId(orgId).badges(parseBadges(org.getBadges())).build();
    }

    @Override
    @SneakyThrows
    public BadgeResponse addBadge(String orgId, TrustBadge badge, String adminId) {
        Organization org = findOrThrow(orgId);
        List<TrustBadge> badges = parseBadges(org.getBadges());
        if (!badges.contains(badge)) badges.add(badge);
        org.setBadges(objectMapper.writeValueAsString(badges));
        orgRepo.save(org);
        return BadgeResponse.builder().organizationId(orgId).badges(badges).build();
    }

    @Override
    @SneakyThrows
    public BadgeResponse removeBadge(String orgId, TrustBadge badge, String adminId) {
        Organization org = findOrThrow(orgId);
        List<TrustBadge> badges = parseBadges(org.getBadges());
        badges.remove(badge);
        org.setBadges(objectMapper.writeValueAsString(badges));
        orgRepo.save(org);
        return BadgeResponse.builder().organizationId(orgId).badges(badges).build();
    }

    @SneakyThrows
    private List<TrustBadge> parseBadges(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        return objectMapper.readValue(json, new TypeReference<List<TrustBadge>>() {});
    }

    private Organization findOrThrow(String id) {
        return orgRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
    }
}
