package tn.esprit.pi.nexlance.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nexlance.entities.FreelanceProfile;
import tn.esprit.pi.nexlance.repositories.FreelanceProfileRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FreelanceProfileService {

    private final FreelanceProfileRepository freelanceProfileRepository;

    @Transactional
    public FreelanceProfile createProfile(FreelanceProfile profile) {
        if (freelanceProfileRepository.existsByUserId(profile.getUserId())) {
            throw new RuntimeException("Profile already exists for this user");
        }
        return freelanceProfileRepository.save(profile);
    }

    public FreelanceProfile getProfileById(UUID id) {
        return freelanceProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile not found with id: " + id));
    }

    public FreelanceProfile getProfileByUserId(UUID userId) {
        return freelanceProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user id: " + userId));
    }

    public Page<FreelanceProfile> getAllProfiles(Pageable pageable) {
        return freelanceProfileRepository.findAll(pageable);
    }

    public Page<FreelanceProfile> getProfilesByAvailability(FreelanceProfile.Availability availability, Pageable pageable) {
        return freelanceProfileRepository.findByAvailability(availability, pageable);
    }

    public Page<FreelanceProfile> getProfilesByLocation(String location, Pageable pageable) {
        return freelanceProfileRepository.findByLocation(location, pageable);
    }

    @Transactional
    public FreelanceProfile updateProfile(UUID id, FreelanceProfile profileDetails) {
        FreelanceProfile profile = getProfileById(id);
        
        if (profileDetails.getTitle() != null) {
            profile.setTitle(profileDetails.getTitle());
        }
        if (profileDetails.getBio() != null) {
            profile.setBio(profileDetails.getBio());
        }
        if (profileDetails.getHourlyRate() != null) {
            profile.setHourlyRate(profileDetails.getHourlyRate());
        }
        if (profileDetails.getAvailability() != null) {
            profile.setAvailability(profileDetails.getAvailability());
        }
        if (profileDetails.getExperienceYears() != null) {
            profile.setExperienceYears(profileDetails.getExperienceYears());
        }
        if (profileDetails.getPortfolioUrl() != null) {
            profile.setPortfolioUrl(profileDetails.getPortfolioUrl());
        }
        if (profileDetails.getLinkedInUrl() != null) {
            profile.setLinkedInUrl(profileDetails.getLinkedInUrl());
        }
        if (profileDetails.getGithubUrl() != null) {
            profile.setGithubUrl(profileDetails.getGithubUrl());
        }
        if (profileDetails.getLocation() != null) {
            profile.setLocation(profileDetails.getLocation());
        }
        if (profileDetails.getLanguages() != null) {
            profile.setLanguages(profileDetails.getLanguages());
        }
        if (profileDetails.getTimezone() != null) {
            profile.setTimezone(profileDetails.getTimezone());
        }
        if (profileDetails.getCompletionRate() != null) {
            profile.setCompletionRate(profileDetails.getCompletionRate());
        }
        if (profileDetails.getResponseTime() != null) {
            profile.setResponseTime(profileDetails.getResponseTime());
        }
        
        return freelanceProfileRepository.save(profile);
    }

    @Transactional
    public void deleteProfile(UUID id) {
        if (!freelanceProfileRepository.existsById(id)) {
            throw new RuntimeException("Profile not found with id: " + id);
        }
        freelanceProfileRepository.deleteById(id);
    }
}
