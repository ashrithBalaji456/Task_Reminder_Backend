package com.application.taskmanager.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscriptionResponse {

    private Long id;
    private Long userId;
    private String endpoint;
    private boolean active;
    private Instant createdAt;
}
