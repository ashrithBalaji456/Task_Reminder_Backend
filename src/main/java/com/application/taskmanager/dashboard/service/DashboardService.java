package com.application.taskmanager.dashboard.service;

import com.application.taskmanager.dashboard.dto.DashboardResponse;
import com.application.taskmanager.exception.ResourceNotFoundException;
import com.application.taskmanager.task.entity.Priority;
import com.application.taskmanager.task.entity.TaskOccurrence;
import com.application.taskmanager.task.entity.TaskStatus;
import com.application.taskmanager.task.repository.TaskOccurrenceRepository;
import com.application.taskmanager.task.service.TaskService;
import com.application.taskmanager.user.entity.User;
import com.application.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;

    @Transactional
    public DashboardResponse getDailyDashboard(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        LocalDate queryDate = date != null ? date : LocalDate.now(ZoneId.of(user.getTimezone()));

        // Materialize occurrences for query date
        taskService.materializeOccurrencesForUserAndDate(user, queryDate);

        List<TaskOccurrence> occurrences = taskOccurrenceRepository.findByUserIdAndOccurrenceDate(userId, queryDate);

        long activeTotalTasks = occurrences.stream().filter(o -> o.getStatus() != TaskStatus.CANCELLED).count();
        long completedTasks = occurrences.stream().filter(o -> o.getStatus() == TaskStatus.COMPLETED).count();
        long pendingTasks = occurrences.stream().filter(o -> o.getStatus() == TaskStatus.PENDING).count();
        long cancelledTasks = occurrences.stream().filter(o -> o.getStatus() == TaskStatus.CANCELLED).count();

        long highPriorityPending = occurrences.stream()
                .filter(o -> o.getStatus() == TaskStatus.PENDING && o.getPriority() == Priority.HIGH)
                .count();

        long mediumPriorityPending = occurrences.stream()
                .filter(o -> o.getStatus() == TaskStatus.PENDING && o.getPriority() == Priority.MEDIUM)
                .count();

        long lowPriorityPending = occurrences.stream()
                .filter(o -> o.getStatus() == TaskStatus.PENDING && o.getPriority() == Priority.LOW)
                .count();

        double completionPercentage = activeTotalTasks > 0
                ? Math.round(((double) completedTasks / activeTotalTasks * 100.0) * 100.0) / 100.0
                : 0.0;

        List<TaskOccurrence> allOccurrences = taskOccurrenceRepository.findAllByUserIdOrdered(userId);
        long allTimeActive = allOccurrences.stream().filter(o -> o.getStatus() != TaskStatus.CANCELLED).count();
        long allTimeCompleted = allOccurrences.stream().filter(o -> o.getStatus() == TaskStatus.COMPLETED).count();
        long allTimePending = allOccurrences.stream().filter(o -> o.getStatus() == TaskStatus.PENDING).count();
        long allTimeCancelled = allOccurrences.stream().filter(o -> o.getStatus() == TaskStatus.CANCELLED).count();
        double allTimeCompletionRate = allTimeActive > 0
                ? Math.round(((double) allTimeCompleted / allTimeActive * 100.0) * 100.0) / 100.0
                : 0.0;

        return DashboardResponse.builder()
                .date(queryDate)
                .totalTasks(activeTotalTasks)
                .completedTasks(completedTasks)
                .pendingTasks(pendingTasks)
                .cancelledTasks(cancelledTasks)
                .highPriorityPendingCount(highPriorityPending)
                .mediumPriorityPendingCount(mediumPriorityPending)
                .lowPriorityPendingCount(lowPriorityPending)
                .completionPercentage(completionPercentage)
                .allTimeTotalTasks(allTimeActive)
                .allTimeCompletedTasks(allTimeCompleted)
                .allTimePendingTasks(allTimePending)
                .allTimeCancelledTasks(allTimeCancelled)
                .allTimeCompletionPercentage(allTimeCompletionRate)
                .build();
    }
}
