package tn.esprit.pi.nexlance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi.nexlance.entities.FreelanceProfile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreelanceProfileDto {
    private UUID id;
    private UUID userId;
    private String title;
    private String bio;
    private BigDecimal hourlyRate;
    private FreelanceProfile.Availability availability;
    private Integer experienceYears;
    private String portfolioUrl;
    private String linkedInUrl;
    private String githubUrl;
    private String location;
    private List<String> languages;
    private String timezone;
    private BigDecimal completionRate;
    private Integer responseTime;
    private List<String> skills;
    private List<String> certifications;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // User info (from joined User entity)
    private String firstName;
    private String lastName;
    private String email;
    private String avatar;
}
