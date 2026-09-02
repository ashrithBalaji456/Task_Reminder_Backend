package com.application.taskmanager.notification.controller;

import com.application.taskmanager.common.ApiResponse;
import com.application.taskmanager.notification.dto.PushSubscriptionRequest;
import com.application.taskmanager.notification.dto.PushSubscriptionResponse;
import com.application.taskmanager.notification.service.PushSubscriptionService;
import com.application.taskmanager.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/push-subscriptions")
@RequiredArgsConstructor
@Tag(name = "Push Subscriptions", description = "Endpoints for registering and managing browser Web Push notification subscriptions")
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;

    @GetMapping("/vapid-public-key")
    @Operation(summary = "Get VAPID Public Key", description = "Returns public key required to subscribe browser for Web Push")
    public ResponseEntity<ApiResponse<Map<String, String>>> getVapidPublicKey() {
        String publicKey = pushSubscriptionService.getVapidPublicKey();
        return ResponseEntity.ok(ApiResponse.success(Map.of("publicKey", publicKey)));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Register Push Subscription", description = "Associates device push subscription endpoint & keys with authenticated user")
    public ResponseEntity<ApiResponse<PushSubscriptionResponse>> registerSubscription(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody PushSubscriptionRequest request) {
        PushSubscriptionResponse response = pushSubscriptionService.saveSubscription(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Push subscription registered successfully", response));
    }

    @DeleteMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Unsubscribe Push Notification", description = "Removes device push subscription for authenticated user")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam("endpoint") String endpoint) {
        pushSubscriptionService.unsubscribe(currentUser.getId(), endpoint);
        return ResponseEntity.ok(ApiResponse.success("Unsubscribed successfully", null));
    }
}
