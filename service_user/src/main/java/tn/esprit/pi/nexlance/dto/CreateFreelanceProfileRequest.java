package tn.esprit.pi.nexlance.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.pi.nexlance.entities.FreelanceProfile;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFreelanceProfileRequest {
    
    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;

    @NotBlank(message = "Bio is required")
    @Size(min = 100, max = 2000, message = "Bio must be between 100 and 2000 characters")
    private String bio;

    @NotNull(message = "Hourly rate is required")
    @DecimalMin(value = "0.0", message = "Hourly rate must be positive")
    private BigDecimal hourlyRate;

    @NotNull(message = "Availability is required")
    private FreelanceProfile.Availability availability;

    @NotNull(message = "Experience years is required")
    @Min(value = 0, message = "Experience years must be non-negative")
    private Integer experienceYears;

    private String portfolioUrl;
    private String linkedInUrl;
    private String githubUrl;

    @NotBlank(message = "Location is required")
    private String location;

    @NotEmpty(message = "At least one language is required")
    private List<String> languages;

    @NotBlank(message = "Timezone is required")
    private String timezone;

    private List<String> skills;
    private List<String> certifications;
}
