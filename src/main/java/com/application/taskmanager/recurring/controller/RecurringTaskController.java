package com.application.taskmanager.recurring.controller;

import com.application.taskmanager.common.ApiResponse;
import com.application.taskmanager.recurring.dto.RecurringTaskResponse;
import com.application.taskmanager.recurring.service.RecurringTaskService;
import com.application.taskmanager.security.UserPrincipal;
import com.application.taskmanager.task.dto.CreateTaskRequest;
import com.application.taskmanager.task.dto.UpdateTaskRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recurring-tasks")
@RequiredArgsConstructor
@Tag(name = "Recurring Tasks", description = "Endpoints for managing recurring task templates, locking, and unlocking")
@SecurityRequirement(name = "bearerAuth")
public class RecurringTaskController {

    private final RecurringTaskService recurringTaskService;

    @PostMapping
    @Operation(summary = "Create recurring task", description = "Creates a daily recurring task template definition")
    public ResponseEntity<ApiResponse<RecurringTaskResponse>> createRecurringTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateTaskRequest request) {
        RecurringTaskResponse response = recurringTaskService.createRecurringTask(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Recurring task created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get recurring tasks", description = "Lists all recurring task definitions for authenticated user")
    public ResponseEntity<ApiResponse<List<RecurringTaskResponse>>> getRecurringTasks(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<RecurringTaskResponse> response = recurringTaskService.getRecurringTasks(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update recurring task template", description = "Updates a recurring task definition")
    public ResponseEntity<ApiResponse<RecurringTaskResponse>> updateRecurringTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        RecurringTaskResponse response = recurringTaskService.updateRecurringTask(currentUser.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Recurring task updated successfully", response));
    }

    @PatchMapping("/{id}/lock")
    @Operation(summary = "Lock recurring task", description = "Locks recurring task template so daily occurrences continue appearing")
    public ResponseEntity<ApiResponse<RecurringTaskResponse>> lockRecurringTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") Long id) {
        RecurringTaskResponse response = recurringTaskService.lockRecurringTask(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Recurring task locked", response));
    }

    @PatchMapping("/{id}/unlock")
    @Operation(summary = "Unlock recurring task", description = "Unlocks recurring task template")
    public ResponseEntity<ApiResponse<RecurringTaskResponse>> unlockRecurringTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") Long id) {
        RecurringTaskResponse response = recurringTaskService.unlockRecurringTask(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Recurring task unlocked", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Stop/Delete recurring task template", description = "Deletes recurring task template definition so future occurrences are no longer created")
    public ResponseEntity<ApiResponse<Void>> deleteRecurringTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") Long id) {
        recurringTaskService.deleteRecurringTask(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Daily recurring template stopped successfully", null));
    }
}
