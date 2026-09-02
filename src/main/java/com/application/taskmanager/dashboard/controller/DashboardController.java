package com.application.taskmanager.dashboard.controller;

import com.application.taskmanager.common.ApiResponse;
import com.application.taskmanager.dashboard.dto.DashboardResponse;
import com.application.taskmanager.dashboard.service.DashboardService;
import com.application.taskmanager.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Endpoints for daily summary statistics and task breakdown")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/daily")
    @Operation(summary = "Get daily statistics", description = "Returns total, completed, pending counts by priority, and completion percentage for a date")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDailyDashboard(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DashboardResponse response = dashboardService.getDailyDashboard(currentUser.getId(), date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
