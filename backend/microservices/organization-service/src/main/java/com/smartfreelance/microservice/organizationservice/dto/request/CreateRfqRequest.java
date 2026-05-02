package com.smartfreelance.microservice.organizationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateRfqRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String description;

    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private LocalDate deadline;
    private List<String> skillsNeeded;
}
