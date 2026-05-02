package com.smartfreelance.microservice.organizationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReplyReviewRequest {

    @NotBlank
    private String reply;
}
