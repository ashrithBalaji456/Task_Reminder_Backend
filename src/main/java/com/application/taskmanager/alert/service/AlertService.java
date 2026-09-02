package com.application.taskmanager.alert.service;

import com.application.taskmanager.alert.dto.AlertResponse;
import com.application.taskmanager.exception.ResourceNotFoundException;
import com.application.taskmanager.task.entity.TaskOccurrence;
import com.application.taskmanager.task.entity.TaskStatus;
import com.application.taskmanager.task.repository.TaskOccurrenceRepository;
import com.application.taskmanager.user.entity.User;
import com.application.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final UserRepository userRepository;

    @Value("${app.reminder.due-soon-window-minutes:30}")
    private int dueSoonWindowMinutes;

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Instant now = Instant.now();
        Instant dueWindowEnd = now.plus(Duration.ofMinutes(dueSoonWindowMinutes));

        List<TaskOccurrence> pendingTasks = taskOccurrenceRepository.findAlertsForUser(
                userId, TaskStatus.PENDING, now, dueWindowEnd
        );

        return pendingTasks.stream().map(occurrence -> {
            long minutesRemaining = Duration.between(now, occurrence.getDueDateTime()).toMinutes();
            return AlertResponse.builder()
                    .id(occurrence.getId())
                    .title(occurrence.getTitle())
                    .description(occurrence.getDescription())
                    .priority(occurrence.getPriority())
                    .dueDate(occurrence.getOccurrenceDate())
                    .dueTime(occurrence.getDueTime())
                    .dueDateTime(occurrence.getDueDateTime())
                    .minutesRemaining(Math.max(0, minutesRemaining))
                    .build();
        }).collect(Collectors.toList());
    }
}
