package tn.esprit.pi.nexlance.controllers;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nexlance.dto.CreateFreelanceProfileRequest;
import tn.esprit.pi.nexlance.dto.FreelanceProfileDto;
import tn.esprit.pi.nexlance.dto.UpdateFreelanceProfileRequest;
import tn.esprit.pi.nexlance.entities.FreelanceProfile;
import tn.esprit.pi.nexlance.mappers.FreelanceProfileMapper;
import tn.esprit.pi.nexlance.services.FreelanceProfileService;
import tn.esprit.pi.nexlance.services.UserService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/freelance-profiles")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202"})
public class FreelanceProfileController {

    private final FreelanceProfileService freelanceProfileService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<FreelanceProfileDto> createProfile(
            @Valid @RequestBody CreateFreelanceProfileRequest request,
            Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            FreelanceProfile profile = FreelanceProfileMapper.toEntity(request);
            profile.setUserId(userId);
            FreelanceProfile createdProfile = freelanceProfileService.createProfile(profile);
            FreelanceProfileDto dto = FreelanceProfileMapper.toDto(createdProfile);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<FreelanceProfileDto> getMyProfile(Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            FreelanceProfile profile = freelanceProfileService.getProfileByUserId(userId);
            FreelanceProfileDto dto = FreelanceProfileMapper.toDto(profile);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/me")
    public ResponseEntity<FreelanceProfileDto> updateMyProfile(
            @Valid @RequestBody UpdateFreelanceProfileRequest request,
            Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            FreelanceProfile currentProfile = freelanceProfileService.getProfileByUserId(userId);
            FreelanceProfileMapper.updateEntityFromRequest(currentProfile, request);
            FreelanceProfile updatedProfile = freelanceProfileService.updateProfile(currentProfile.getId(), currentProfile);
            FreelanceProfileDto dto = FreelanceProfileMapper.toDto(updatedProfile);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PatchMapping("/me/availability")
    public ResponseEntity<FreelanceProfileDto> updateMyAvailability(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            FreelanceProfile profile = freelanceProfileService.getProfileByUserId(userId);
            String availabilityStr = request.get("availability");
            FreelanceProfile.Availability availability = FreelanceProfile.Availability.valueOf(availabilityStr);
            profile.setAvailability(availability);
            FreelanceProfile updatedProfile = freelanceProfileService.updateProfile(profile.getId(), profile);
            FreelanceProfileDto dto = FreelanceProfileMapper.toDto(updatedProfile);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<FreelanceProfileDto> getProfileById(@PathVariable UUID id) {
        try {
            FreelanceProfile profile = freelanceProfileService.getProfileById(id);
            FreelanceProfileDto dto = FreelanceProfileMapper.toDto(profile);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<FreelanceProfileDto> getProfileByUserId(@PathVariable UUID userId) {
        try {
            FreelanceProfile profile = freelanceProfileService.getProfileByUserId(userId);
            FreelanceProfileDto dto = FreelanceProfileMapper.toDto(profile);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<FreelanceProfileDto>> getAllProfiles(Pageable pageable) {
        Page<FreelanceProfile> profiles = freelanceProfileService.getAllProfiles(pageable);
        Page<FreelanceProfileDto> dtos = profiles.map(FreelanceProfileMapper::toDto);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/search/availability/{availability}")
    public ResponseEntity<Page<FreelanceProfileDto>> getProfilesByAvailability(
            @PathVariable FreelanceProfile.Availability availability,
            Pageable pageable) {
        Page<FreelanceProfile> profiles = freelanceProfileService.getProfilesByAvailability(availability, pageable);
        Page<FreelanceProfileDto> dtos = profiles.map(FreelanceProfileMapper::toDto);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/search/location/{location}")
    public ResponseEntity<Page<FreelanceProfileDto>> getProfilesByLocation(
            @PathVariable String location,
            Pageable pageable) {
        Page<FreelanceProfile> profiles = freelanceProfileService.getProfilesByLocation(location, pageable);
        Page<FreelanceProfileDto> dtos = profiles.map(FreelanceProfileMapper::toDto);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/me/exists")
    public ResponseEntity<Map<String, Boolean>> checkMyProfileExists(Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            freelanceProfileService.getProfileByUserId(userId);
            Map<String, Boolean> response = new HashMap<>();
            response.put("exists", true);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Boolean> response = new HashMap<>();
            response.put("exists", false);
            return ResponseEntity.ok(response);
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyProfile(Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            FreelanceProfile profile = freelanceProfileService.getProfileByUserId(userId);
            freelanceProfileService.deleteProfile(profile.getId());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null) {
            Object details = authentication.getDetails();
            if (details instanceof Claims claims) {
                String userId = claims.get("userId", String.class);
                if (userId != null) return UUID.fromString(userId);
            }
            try {
                return UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException e) {
                return userService.getUserByEmail(authentication.getName()).getId();
            }
        }
        throw new RuntimeException("Unable to extract user ID from authentication");
    }
}