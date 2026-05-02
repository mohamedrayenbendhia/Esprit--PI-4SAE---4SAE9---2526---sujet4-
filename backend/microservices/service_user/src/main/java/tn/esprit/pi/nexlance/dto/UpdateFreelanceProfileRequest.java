package tn.esprit.pi.nexlance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi.nexlance.entities.FreelanceProfile;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFreelanceProfileRequest {
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
    private List<String> skills;
    private List<String> certifications;
}
