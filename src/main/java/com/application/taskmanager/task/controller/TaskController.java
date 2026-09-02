package com.application.taskmanager.task.controller;

import com.application.taskmanager.common.ApiResponse;
import com.application.taskmanager.security.UserPrincipal;
import com.application.taskmanager.task.dto.CreateTaskRequest;
import com.application.taskmanager.task.dto.MoveTaskRequest;
import com.application.taskmanager.task.dto.TaskResponse;
import com.application.taskmanager.task.dto.UpdateTaskRequest;
import com.application.taskmanager.task.entity.Priority;
import com.application.taskmanager.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Endpoints for managing task creation, completion, movement, and filtering")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a new task", description = "Creates a one-time or daily recurring task")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.createTask(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID", description = "Fetches task occurrence by ID for authenticated user")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") Long id) {
        TaskResponse response = taskService.getTaskById(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get tasks for date", description = "Fetches all tasks for a given date (defaults to today)")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksForDate(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        List<TaskResponse> response = taskService.getTasksForDate(currentUser.getId(), queryDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/today")
    @Operation(summary = "Get today's tasks", description = "Returns all task occurrences for today")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTodayTasks(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<TaskResponse> response = taskService.getTodayTasks(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/tomorrow")
    @Operation(summary = "Get tomorrow's tasks", description = "Returns all task occurrences for tomorrow")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTomorrowTasks(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<TaskResponse> response = taskService.getTomorrowTasks(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending tasks", description = "Fetches pending tasks with optional filtering and pagination")
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getPendingTasks(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "priority", required = false) Priority priority,
            @RequestParam(value = "recurring", required = false) Boolean recurring,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TaskResponse> response = taskService.getPendingTasks(
                currentUser.getId(), date, startDate, endDate, priority, recurring, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task", description = "Updates an existing task occurrence and definition")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        TaskResponse response = taskService.updateTask(currentUser.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete task", description = "Deletes a task occurrence")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") Long id) {
        taskService.deleteTask(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Mark task as completed", description = "Marks a task occurrence as completed")
    public ResponseEntity<ApiResponse<TaskResponse>> completeTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") Long id) {
        TaskResponse response = taskService.completeTask(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Task marked as completed", response));
    }

    @PatchMapping("/{id}/move")
    @Operation(summary = "Move task to specific date", description = "Moves pending task to a target date while preserving history")
    public ResponseEntity<ApiResponse<TaskResponse>> moveTask(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody MoveTaskRequest request) {
        TaskResponse response = taskService.moveTaskToDate(currentUser.getId(), id, request.getTargetDate());
        return ResponseEntity.ok(ApiResponse.success("Task moved successfully", response));
    }

    @PatchMapping("/{id}/move/tomorrow")
    @Operation(summary = "Move task to tomorrow", description = "Moves pending task to tomorrow while preserving history")
    public ResponseEntity<ApiResponse<TaskResponse>> moveTaskToTomorrow(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") Long id) {
        TaskResponse response = taskService.moveTaskToTomorrow(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Task moved to tomorrow", response));
    }
}
