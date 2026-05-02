package com.smartfreelance.microservice.complaintservice.dto.advanced;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NpsResponseRequest {
    @NotNull @Min(0) @Max(10)
    private Integer score;
    private String comment;
}
