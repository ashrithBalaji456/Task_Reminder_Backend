package com.application.taskmanager.alert.controller;

import com.application.taskmanager.alert.dto.AlertResponse;
import com.application.taskmanager.alert.service.AlertService;
import com.application.taskmanager.common.ApiResponse;
import com.application.taskmanager.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Endpoints for retrieving urgent alerts and tasks due within 30 minutes")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Get task alerts", description = "Returns pending tasks due within the next 30 minutes for the authenticated user")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAlerts(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<AlertResponse> response = alertService.getAlertsForUser(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
