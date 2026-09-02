package com.application.taskmanager.history.controller;

import com.application.taskmanager.common.ApiResponse;
import com.application.taskmanager.history.dto.DailyHistoryResponse;
import com.application.taskmanager.history.service.HistoryService;
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
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
@Tag(name = "History", description = "Endpoints for viewing historical task completions and status records")
@SecurityRequirement(name = "bearerAuth")
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping
    @Operation(summary = "Get daily task history", description = "Returns total, completed, pending, cancelled tasks and completion percentage for a given date")
    public ResponseEntity<ApiResponse<DailyHistoryResponse>> getDailyHistory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyHistoryResponse response = historyService.getDailyHistory(currentUser.getId(), date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
