package com.application.taskmanager.analytics.controller;

import com.application.taskmanager.analytics.dto.MonthlyAnalyticsResponse;
import com.application.taskmanager.analytics.dto.WeeklyAnalyticsResponse;
import com.application.taskmanager.analytics.service.AnalyticsService;
import com.application.taskmanager.common.ApiResponse;
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
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Endpoints for retrieving weekly and monthly task completion analytics")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/weekly")
    @Operation(summary = "Get weekly analytics", description = "Returns task completion statistics and trends for the previous completed week")
    public ResponseEntity<ApiResponse<WeeklyAnalyticsResponse>> getWeeklyAnalytics(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        WeeklyAnalyticsResponse response = analyticsService.getWeeklyAnalytics(currentUser.getId(), date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/monthly")
    @Operation(summary = "Get monthly analytics", description = "Returns task completion statistics, priority breakdown, and trends for the previous completed month")
    public ResponseEntity<ApiResponse<MonthlyAnalyticsResponse>> getMonthlyAnalytics(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        MonthlyAnalyticsResponse response = analyticsService.getMonthlyAnalytics(currentUser.getId(), date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
