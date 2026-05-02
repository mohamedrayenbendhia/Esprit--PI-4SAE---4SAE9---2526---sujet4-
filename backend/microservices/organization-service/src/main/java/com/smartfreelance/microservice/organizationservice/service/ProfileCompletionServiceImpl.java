package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.response.ProfileCompletionResponse;
import com.smartfreelance.microservice.organizationservice.entity.Organization;
import com.smartfreelance.microservice.organizationservice.enums.MemberRole;
import com.smartfreelance.microservice.organizationservice.enums.MemberStatus;
import com.smartfreelance.microservice.organizationservice.exception.ResourceNotFoundException;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationMemberRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrganizationRepository;
import com.smartfreelance.microservice.organizationservice.repository.OrgPortfolioItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileCompletionServiceImpl implements ProfileCompletionService {

    private final OrganizationRepository       orgRepo;
    private final OrganizationMemberRepository memberRepo;
    private final OrgPortfolioItemRepository   portfolioRepo;

    @Override
    public ProfileCompletionResponse compute(String orgId) {
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable : " + orgId));

        return buildResponse(org);
    }

    @Override
    public int computeScore(Organization org) {
        return buildResponse(org).getScore();
    }

    // ── Logique interne ───────────────────────────────────────────────────────

    private ProfileCompletionResponse buildResponse(Organization org) {
        String orgId = org.getId();

        // ── Critères ─────────────────────────────────────────────────────────
        boolean hasDescription  = notBlank(org.getDescription());
        boolean hasLogo         = notBlank(org.getLogoUrl());
        boolean hasWebsite      = notBlank(org.getWebsite());
        boolean hasSpecialties  = org.getSpecialties() != null && org.getSpecialties().size() >= 3;
        boolean hasSiret        = notBlank(org.getSiret());
        boolean hasLocation     = notBlank(org.getLocation());
        boolean hasPortfolio    = portfolioRepo.countByOrganizationId(orgId) >= 1;
        boolean hasTeamMember   = memberRepo
                .findByOrganizationIdAndRole(orgId, MemberRole.MANAGER).size() > 0
                || memberRepo.findByOrganizationIdAndRole(orgId, MemberRole.MEMBER)
                       .stream().anyMatch(m -> m.getStatus() == MemberStatus.ACTIVE);

        // ── Score ─────────────────────────────────────────────────────────────
        int score = 0;
        if (hasDescription) score += 15;
        if (hasLogo)        score += 10;
        if (hasWebsite)     score += 10;
        if (hasSpecialties) score += 15;
        if (hasSiret)       score += 10;
        if (hasLocation)    score += 10;
        if (hasPortfolio)   score += 15;
        if (hasTeamMember)  score += 15;

        // ── Conseils pour compléter ───────────────────────────────────────────
        List<String> missing = new ArrayList<>();
        if (!hasDescription) missing.add("Ajoutez une description (15 pts)");
        if (!hasLogo)        missing.add("Ajoutez un logo (10 pts)");
        if (!hasWebsite)     missing.add("Renseignez votre site web (10 pts)");
        if (!hasSpecialties) missing.add("Ajoutez au moins 3 spécialités (15 pts)");
        if (!hasSiret)       missing.add("Renseignez votre SIRET (10 pts)");
        if (!hasLocation)    missing.add("Indiquez votre localisation (10 pts)");
        if (!hasPortfolio)   missing.add("Ajoutez au moins un projet au portfolio (15 pts)");
        if (!hasTeamMember)  missing.add("Invitez un membre dans votre équipe (15 pts)");

        return ProfileCompletionResponse.builder()
                .organizationId(orgId)
                .organizationName(org.getName())
                .score(score)
                .visibleInSearch(score >= 40)
                .breakdown(ProfileCompletionResponse.Breakdown.builder()
                        .hasDescription(hasDescription)
                        .hasLogo(hasLogo)
                        .hasWebsite(hasWebsite)
                        .hasSpecialties(hasSpecialties)
                        .hasSiret(hasSiret)
                        .hasLocation(hasLocation)
                        .hasPortfolio(hasPortfolio)
                        .hasTeamMember(hasTeamMember)
                        .missingItems(missing)
                        .build())
                .build();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
