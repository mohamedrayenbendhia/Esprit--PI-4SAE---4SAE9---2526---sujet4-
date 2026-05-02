package tn.esprit.pi.nexlance.mappers;

import tn.esprit.pi.nexlance.dto.FreelanceProfileDto;
import tn.esprit.pi.nexlance.dto.CreateFreelanceProfileRequest;
import tn.esprit.pi.nexlance.dto.UpdateFreelanceProfileRequest;
import tn.esprit.pi.nexlance.entities.FreelanceProfile;
import tn.esprit.pi.nexlance.entities.User;

import java.math.BigDecimal;

public class FreelanceProfileMapper {

    public static FreelanceProfileDto toDto(FreelanceProfile profile) {
        if (profile == null) {
            return null;
        }

        FreelanceProfileDto dto = new FreelanceProfileDto();
        dto.setId(profile.getId());
        dto.setUserId(profile.getUserId());
        dto.setTitle(profile.getTitle());
        dto.setBio(profile.getBio());
        dto.setHourlyRate(profile.getHourlyRate());
        dto.setAvailability(profile.getAvailability());
        dto.setExperienceYears(profile.getExperienceYears());
        dto.setPortfolioUrl(profile.getPortfolioUrl());
        dto.setLinkedInUrl(profile.getLinkedInUrl());
        dto.setGithubUrl(profile.getGithubUrl());
        dto.setLocation(profile.getLocation());
        dto.setLanguages(profile.getLanguages());
        dto.setTimezone(profile.getTimezone());
        dto.setCompletionRate(profile.getCompletionRate());
        dto.setResponseTime(profile.getResponseTime());
        dto.setSkills(profile.getSkills());
        dto.setCertifications(profile.getCertifications());
        dto.setCreatedAt(profile.getCreatedAt());
        dto.setUpdatedAt(profile.getUpdatedAt());

        // Add user info if available
        if (profile.getUser() != null) {
            User user = profile.getUser();
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setAvatar(user.getAvatar());
        }

        return dto;
    }

    public static FreelanceProfile toEntity(CreateFreelanceProfileRequest request) {
        if (request == null) {
            return null;
        }

        FreelanceProfile profile = new FreelanceProfile();
        profile.setTitle(request.getTitle());
        profile.setBio(request.getBio());
        profile.setHourlyRate(request.getHourlyRate());
        profile.setAvailability(request.getAvailability());
        profile.setExperienceYears(request.getExperienceYears());
        profile.setPortfolioUrl(request.getPortfolioUrl());
        profile.setLinkedInUrl(request.getLinkedInUrl());
        profile.setGithubUrl(request.getGithubUrl());
        profile.setLocation(request.getLocation());
        profile.setLanguages(request.getLanguages());
        profile.setTimezone(request.getTimezone());
        profile.setSkills(request.getSkills());
        profile.setCertifications(request.getCertifications());
        profile.setCompletionRate(BigDecimal.ZERO);
        profile.setResponseTime(0);

        return profile;
    }

    public static void updateEntityFromRequest(FreelanceProfile profile, UpdateFreelanceProfileRequest request) {
        if (profile == null || request == null) {
            return;
        }

        if (request.getTitle() != null) {
            profile.setTitle(request.getTitle());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getHourlyRate() != null) {
            profile.setHourlyRate(request.getHourlyRate());
        }
        if (request.getAvailability() != null) {
            profile.setAvailability(request.getAvailability());
        }
        if (request.getExperienceYears() != null) {
            profile.setExperienceYears(request.getExperienceYears());
        }
        if (request.getPortfolioUrl() != null) {
            profile.setPortfolioUrl(request.getPortfolioUrl());
        }
        if (request.getLinkedInUrl() != null) {
            profile.setLinkedInUrl(request.getLinkedInUrl());
        }
        if (request.getGithubUrl() != null) {
            profile.setGithubUrl(request.getGithubUrl());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation());
        }
        if (request.getLanguages() != null) {
            profile.setLanguages(request.getLanguages());
        }
        if (request.getTimezone() != null) {
            profile.setTimezone(request.getTimezone());
        }
        if (request.getSkills() != null) {
            profile.setSkills(request.getSkills());
        }
        if (request.getCertifications() != null) {
            profile.setCertifications(request.getCertifications());
        }
    }
}
