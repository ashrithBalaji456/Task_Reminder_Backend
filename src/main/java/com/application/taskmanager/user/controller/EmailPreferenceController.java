package com.application.taskmanager.user.controller;

import com.application.taskmanager.common.ApiResponse;
import com.application.taskmanager.security.UserPrincipal;
import com.application.taskmanager.user.dto.UserEmailPreferenceRequest;
import com.application.taskmanager.user.dto.UserEmailPreferenceResponse;
import com.application.taskmanager.user.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/email-preferences")
@RequiredArgsConstructor
@Tag(name = "Email Preferences", description = "Endpoints for viewing and updating user email notification settings")
@SecurityRequirement(name = "bearerAuth")
public class EmailPreferenceController {

    private final UserPreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Get user email preferences", description = "Returns email notification toggles and weekly report schedule preferences")
    public ResponseEntity<ApiResponse<UserEmailPreferenceResponse>> getPreferences(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UserEmailPreferenceResponse response = preferenceService.getUserPreferences(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping
    @Operation(summary = "Update user email preferences", description = "Updates email notification toggles, report times, and user timezone")
    public ResponseEntity<ApiResponse<UserEmailPreferenceResponse>> updatePreferences(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UserEmailPreferenceRequest request) {
        UserEmailPreferenceResponse response = preferenceService.updateUserPreferences(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Email preferences updated successfully", response));
    }
}
