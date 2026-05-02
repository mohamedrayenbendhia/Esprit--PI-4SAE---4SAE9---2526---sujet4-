package com.microservice.module_portfolio.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ProjectRequest {

    @NotBlank(message = "Project title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private String techStack;
    private LocalDate startDate;
    private LocalDate endDate;

    @Pattern(regexp = "^$|https?://.+", message = "Invalid URL format")
    private String githubUrl;

    @Pattern(regexp = "^$|https?://.+", message = "Invalid URL format")
    private String demoUrl;

    @Pattern(regexp = "^$|https?://.+", message = "Invalid URL format")
    private String images;

    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateValid() {
        if (startDate == null || endDate == null) return true;
        return endDate.isAfter(startDate);
    }
}