package com.application.taskmanager.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscriptionRequest {

    @NotBlank(message = "endpoint is required")
    private String endpoint;

    @NotBlank(message = "p256dhKey is required")
    private String p256dhKey;

    @NotBlank(message = "authKey is required")
    private String authKey;
}
