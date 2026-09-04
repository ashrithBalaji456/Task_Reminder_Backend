package com.application.taskmanager.history.service;

import com.application.taskmanager.exception.ResourceNotFoundException;
import com.application.taskmanager.history.dto.DailyHistoryResponse;
import com.application.taskmanager.task.dto.TaskResponse;
import com.application.taskmanager.task.entity.TaskOccurrence;
import com.application.taskmanager.task.entity.TaskStatus;
import com.application.taskmanager.task.repository.TaskOccurrenceRepository;
import com.application.taskmanager.task.service.TaskMapper;
import com.application.taskmanager.task.service.TaskService;
import com.application.taskmanager.user.entity.User;
import com.application.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final TaskMapper taskMapper;

    @Transactional
    public DailyHistoryResponse getDailyHistory(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<TaskOccurrence> occurrences;

        if (date != null) {
            // Materialize occurrences for specific requested date
            taskService.materializeOccurrencesForUserAndDate(user, date);
            occurrences = taskOccurrenceRepository.findByUserIdAndOccurrenceDateOrdered(userId, date);
        } else {
            // Materialize occurrences for today as well
            taskService.materializeOccurrencesForUserAndDate(user, LocalDate.now(java.time.ZoneId.of(user.getTimezone())));
            occurrences = taskOccurrenceRepository.findAllByUserIdOrdered(userId);
        }

        long activeTotalTasks = occurrences.stream().filter(o -> o.getStatus() != TaskStatus.CANCELLED).count();
        long completedTasks = occurrences.stream().filter(o -> o.getStatus() == TaskStatus.COMPLETED).count();
        long pendingTasks = occurrences.stream().filter(o -> o.getStatus() == TaskStatus.PENDING).count();
        long cancelledTasks = occurrences.stream().filter(o -> o.getStatus() == TaskStatus.CANCELLED).count();

        double completionPercentage = activeTotalTasks > 0
                ? Math.round(((double) completedTasks / activeTotalTasks * 100.0) * 100.0) / 100.0
                : 0.0;

        List<TaskResponse> taskResponses = occurrences.stream()
                .filter(o -> o.getStatus() != TaskStatus.CANCELLED)
                .map(taskMapper::toTaskResponse)
                .collect(Collectors.toList());

        return DailyHistoryResponse.builder()
                .date(date) // null when viewing all history
                .totalTasks(activeTotalTasks)
                .completedTasks(completedTasks)
                .pendingTasks(pendingTasks)
                .cancelledTasks(cancelledTasks)
                .completionPercentage(completionPercentage)
                .tasks(taskResponses)
                .build();
    }
}
