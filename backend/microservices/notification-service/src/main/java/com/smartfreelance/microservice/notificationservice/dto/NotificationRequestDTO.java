package com.smartfreelance.microservice.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDTO {

    @NotBlank(message = "recipientId is required")
    @Size(max = 36)
    private String recipientId;

    @NotBlank(message = "type is required")
    @Size(max = 50)
    private String type;

    @NotBlank(message = "title is required")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "message is required")
    @Size(max = 500)
    private String message;

    @Size(max = 36)
    private String referenceId;

    @Size(max = 50)
    private String referenceType;
}
