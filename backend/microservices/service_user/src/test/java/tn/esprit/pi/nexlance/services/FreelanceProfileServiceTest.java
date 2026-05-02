package tn.esprit.pi.nexlance.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tn.esprit.pi.nexlance.entities.FreelanceProfile;
import tn.esprit.pi.nexlance.repositories.FreelanceProfileRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreelanceProfileServiceTest {

    @Mock
    private FreelanceProfileRepository freelanceProfileRepository;

    @InjectMocks
    private FreelanceProfileService freelanceProfileService;

    private FreelanceProfile profile;
    private UUID profileId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        profileId = UUID.randomUUID();
        userId = UUID.randomUUID();
        profile = new FreelanceProfile();
        profile.setId(profileId);
        profile.setUserId(userId);
        profile.setTitle("Java Developer");
        profile.setBio("Experienced Java developer");
        profile.setHourlyRate(BigDecimal.valueOf(50));
        profile.setAvailability(FreelanceProfile.Availability.AVAILABLE);
        profile.setExperienceYears(5);
        profile.setLocation("Paris");
    }

    @Test
    void createProfile_Success() {
        when(freelanceProfileRepository.existsByUserId(userId)).thenReturn(false);
        when(freelanceProfileRepository.save(any(FreelanceProfile.class))).thenReturn(profile);

        FreelanceProfile result = freelanceProfileService.createProfile(profile);

        assertNotNull(result);
        assertEquals("Java Developer", result.getTitle());
        verify(freelanceProfileRepository).save(any(FreelanceProfile.class));
    }

    @Test
    void createProfile_AlreadyExists_ThrowsException() {
        when(freelanceProfileRepository.existsByUserId(userId)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> freelanceProfileService.createProfile(profile));

        assertEquals("Profile already exists for this user", exception.getMessage());
        verify(freelanceProfileRepository, never()).save(any(FreelanceProfile.class));
    }

    @Test
    void getProfileById_Success() {
        when(freelanceProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        FreelanceProfile result = freelanceProfileService.getProfileById(profileId);

        assertNotNull(result);
        assertEquals(profileId, result.getId());
    }

    @Test
    void getProfileById_NotFound_ThrowsException() {
        when(freelanceProfileRepository.findById(profileId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> freelanceProfileService.getProfileById(profileId));
    }

    @Test
    void getProfileByUserId_Success() {
        when(freelanceProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        FreelanceProfile result = freelanceProfileService.getProfileByUserId(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
    }

    @Test
    void getAllProfiles_ReturnsPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<FreelanceProfile> page = new PageImpl<>(List.of(profile));
        when(freelanceProfileRepository.findAll(pageable)).thenReturn(page);

        Page<FreelanceProfile> result = freelanceProfileService.getAllProfiles(pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getProfilesByAvailability_ReturnsFilteredResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<FreelanceProfile> page = new PageImpl<>(List.of(profile));
        when(freelanceProfileRepository.findByAvailability(FreelanceProfile.Availability.AVAILABLE, pageable))
                .thenReturn(page);

        Page<FreelanceProfile> result = freelanceProfileService
                .getProfilesByAvailability(FreelanceProfile.Availability.AVAILABLE, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getProfilesByLocation_ReturnsFilteredResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<FreelanceProfile> page = new PageImpl<>(List.of(profile));
        when(freelanceProfileRepository.findByLocation("Paris", pageable)).thenReturn(page);

        Page<FreelanceProfile> result = freelanceProfileService.getProfilesByLocation("Paris", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void updateProfile_Success() {
        FreelanceProfile updateDetails = new FreelanceProfile();
        updateDetails.setTitle("Senior Java Developer");
        updateDetails.setHourlyRate(BigDecimal.valueOf(75));
        updateDetails.setAvailability(FreelanceProfile.Availability.BUSY);

        when(freelanceProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(freelanceProfileRepository.save(any(FreelanceProfile.class))).thenReturn(profile);

        FreelanceProfile result = freelanceProfileService.updateProfile(profileId, updateDetails);

        assertNotNull(result);
        verify(freelanceProfileRepository).save(any(FreelanceProfile.class));
    }

    @Test
    void deleteProfile_Success() {
        when(freelanceProfileRepository.existsById(profileId)).thenReturn(true);

        freelanceProfileService.deleteProfile(profileId);

        verify(freelanceProfileRepository).deleteById(profileId);
    }

    @Test
    void deleteProfile_NotFound_ThrowsException() {
        when(freelanceProfileRepository.existsById(profileId)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> freelanceProfileService.deleteProfile(profileId));

        verify(freelanceProfileRepository, never()).deleteById(any());
    }
}
