package com.application.taskmanager.notification.controller;

import com.application.taskmanager.common.ApiResponse;
import com.application.taskmanager.notification.dto.NotificationLogResponse;
import com.application.taskmanager.notification.entity.NotificationType;
import com.application.taskmanager.notification.service.NotificationHistoryService;
import com.application.taskmanager.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification History", description = "Endpoints for viewing user email notification logs")
@SecurityRequirement(name = "bearerAuth")
public class NotificationHistoryController {

    private final NotificationHistoryService notificationHistoryService;

    @GetMapping
    @Operation(summary = "Get notification history", description = "Fetches historical email notification logs for the authenticated user")
    public ResponseEntity<ApiResponse<Page<NotificationLogResponse>>> getNotificationHistory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(value = "type", required = false) NotificationType type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationLogResponse> response = notificationHistoryService.getNotificationHistory(
                currentUser.getId(), type, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
