package com.smartfreelance.microservice.organizationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RfqResponseRequest {

    @NotBlank
    private String responseMessage;
}
