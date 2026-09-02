package com.application.taskmanager.task.service;

import com.application.taskmanager.exception.InvalidTaskOperationException;
import com.application.taskmanager.exception.ResourceNotFoundException;
import com.application.taskmanager.task.dto.CreateTaskRequest;
import com.application.taskmanager.task.dto.TaskResponse;
import com.application.taskmanager.task.dto.UpdateTaskRequest;
import com.application.taskmanager.task.entity.*;
import com.application.taskmanager.task.repository.TaskDefinitionRepository;
import com.application.taskmanager.task.repository.TaskMovementHistoryRepository;
import com.application.taskmanager.task.repository.TaskOccurrenceRepository;
import com.application.taskmanager.user.entity.User;
import com.application.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {

    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final TaskMovementHistoryRepository movementHistoryRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    @Transactional
    public TaskResponse createTask(Long userId, CreateTaskRequest request) {
        User user = getUserOrThrow(userId);

        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            String normTz = com.application.taskmanager.user.model.AppTimezone.normalize(request.getTimezone());
            user.setTimezone(normTz);
            userRepository.save(user);
        }

        TaskType type = (request.isRecurring() || request.getTaskType() == TaskType.DAILY_RECURRING)
                ? TaskType.DAILY_RECURRING : TaskType.ONE_TIME;
        RecurrenceType recType = (type == TaskType.DAILY_RECURRING)
                ? RecurrenceType.DAILY : RecurrenceType.NONE;

        ReminderOption remOption = request.getReminderOption() != null ? request.getReminderOption() : ReminderOption.NONE;

        TaskDefinition definition = TaskDefinition.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .taskType(type)
                .recurrenceType(recType)
                .startDate(request.getDueDate())
                .dueTime(request.getDueTime())
                .reminderOption(remOption)
                .customReminderMinutes(request.getCustomReminderMinutes())
                .locked(false)
                .build();

        TaskDefinition savedDefinition = taskDefinitionRepository.save(definition);

        Instant dueDateTime = computeDueDateTime(request.getDueDate(), request.getDueTime(), user.getTimezone());
        Instant reminderScheduledAt = calculateReminderScheduledAt(dueDateTime, remOption, request.getCustomReminderMinutes());

        TaskOccurrence occurrence = TaskOccurrence.builder()
                .taskDefinition(savedDefinition)
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .occurrenceDate(request.getDueDate())
                .dueTime(request.getDueTime())
                .dueDateTime(dueDateTime)
                .reminderOption(remOption)
                .customReminderMinutes(request.getCustomReminderMinutes())
                .reminderScheduledAt(reminderScheduledAt)
                .status(TaskStatus.PENDING)
                .build();

        TaskOccurrence savedOccurrence = taskOccurrenceRepository.save(occurrence);
        log.info("Created task definition id {} and occurrence id {} for user id {}", savedDefinition.getId(), savedOccurrence.getId(), userId);

        return taskMapper.toTaskResponse(savedOccurrence);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long userId, Long occurrenceId) {
        TaskOccurrence occurrence = taskOccurrenceRepository.findByIdAndUserId(occurrenceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + occurrenceId));
        return taskMapper.toTaskResponse(occurrence);
    }

    @Transactional
    public List<TaskResponse> getTasksForDate(Long userId, LocalDate date) {
        User user = getUserOrThrow(userId);
        materializeOccurrencesForUserAndDate(user, date);

        List<TaskOccurrence> occurrences = taskOccurrenceRepository.findByUserIdAndOccurrenceDateOrdered(userId, date);
        return occurrences.stream().map(taskMapper::toTaskResponse).collect(Collectors.toList());
    }

    @Transactional
    public List<TaskResponse> getTodayTasks(Long userId) {
        User user = getUserOrThrow(userId);
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));
        return getTasksForDate(userId, today);
    }

    @Transactional
    public List<TaskResponse> getTomorrowTasks(Long userId) {
        User user = getUserOrThrow(userId);
        LocalDate tomorrow = LocalDate.now(ZoneId.of(user.getTimezone())).plusDays(1);
        return getTasksForDate(userId, tomorrow);
    }

    @Transactional
    public Page<TaskResponse> getPendingTasks(
            Long userId,
            LocalDate date,
            LocalDate startDate,
            LocalDate endDate,
            Priority priority,
            Boolean recurring,
            Pageable pageable
    ) {
        User user = getUserOrThrow(userId);
        LocalDate userToday = LocalDate.now(ZoneId.of(user.getTimezone()));

        if (date != null) {
            materializeOccurrencesForUserAndDate(user, date);
        } else {
            materializeOccurrencesForUserAndDate(user, userToday);
        }

        List<TaskOccurrence> pendingList = taskOccurrenceRepository.findByUserIdAndStatus(userId, TaskStatus.PENDING);

        List<TaskOccurrence> filtered = pendingList.stream()
                .filter(o -> {
                    if (date != null && !o.getOccurrenceDate().equals(date)) return false;
                    if (startDate != null && o.getOccurrenceDate().isBefore(startDate)) return false;
                    if (endDate != null && o.getOccurrenceDate().isAfter(endDate)) return false;
                    if (priority != null && o.getPriority() != priority) return false;
                    if (recurring != null) {
                        boolean isRec = o.getTaskDefinition() != null && o.getTaskDefinition().getTaskType() == TaskType.DAILY_RECURRING;
                        if (isRec != recurring) return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparingInt((TaskOccurrence o) -> o.getPriority().getRank())
                        .thenComparing(TaskOccurrence::getDueTime)
                        .thenComparing(TaskOccurrence::getOccurrenceDate))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());

        List<TaskResponse> pageContent = (start <= filtered.size())
                ? filtered.subList(start, end).stream().map(taskMapper::toTaskResponse).collect(Collectors.toList())
                : List.of();

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Transactional
    public TaskResponse updateTask(Long userId, Long occurrenceId, UpdateTaskRequest request) {
        User user = getUserOrThrow(userId);
        TaskOccurrence occurrence = taskOccurrenceRepository.findByIdAndUserId(occurrenceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task occurrence not found with id: " + occurrenceId));

        LocalDate newDate = request.getDueDate() != null ? request.getDueDate() : occurrence.getOccurrenceDate();
        Instant dueDateTime = computeDueDateTime(newDate, request.getDueTime(), user.getTimezone());

        ReminderOption remOption = request.getReminderOption() != null ? request.getReminderOption() : occurrence.getReminderOption();
        Integer customMins = request.getCustomReminderMinutes() != null ? request.getCustomReminderMinutes() : occurrence.getCustomReminderMinutes();
        Instant reminderScheduledAt = calculateReminderScheduledAt(dueDateTime, remOption, customMins);

        occurrence.setTitle(request.getTitle());
        occurrence.setDescription(request.getDescription());
        occurrence.setPriority(request.getPriority());
        occurrence.setOccurrenceDate(newDate);
        occurrence.setDueTime(request.getDueTime());
        occurrence.setDueDateTime(dueDateTime);
        occurrence.setReminderOption(remOption);
        occurrence.setCustomReminderMinutes(customMins);
        occurrence.setReminderScheduledAt(reminderScheduledAt);

        TaskOccurrence updated = taskOccurrenceRepository.save(occurrence);

        // Update task definition if standalone or definition edit requested
        if (occurrence.getTaskDefinition() != null) {
            TaskDefinition def = occurrence.getTaskDefinition();
            def.setTitle(request.getTitle());
            def.setDescription(request.getDescription());
            def.setPriority(request.getPriority());
            def.setDueTime(request.getDueTime());
            def.setReminderOption(remOption);
            def.setCustomReminderMinutes(customMins);
            taskDefinitionRepository.save(def);
        }

        return taskMapper.toTaskResponse(updated);
    }

    @Transactional
    public void deleteTask(Long userId, Long occurrenceId) {
        TaskOccurrence occurrence = taskOccurrenceRepository.findByIdAndUserId(occurrenceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task occurrence not found with id: " + occurrenceId));

        TaskDefinition def = occurrence.getTaskDefinition();
        taskOccurrenceRepository.delete(occurrence);

        if (def != null && def.getTaskType() == TaskType.ONE_TIME) {
            taskDefinitionRepository.delete(def);
        }
    }

    @Transactional
    public TaskResponse completeTask(Long userId, Long occurrenceId) {
        TaskOccurrence occurrence = taskOccurrenceRepository.findByIdAndUserId(occurrenceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task occurrence not found with id: " + occurrenceId));

        if (occurrence.getStatus() == TaskStatus.COMPLETED) {
            return taskMapper.toTaskResponse(occurrence);
        }

        occurrence.setStatus(TaskStatus.COMPLETED);
        occurrence.setCompletedAt(Instant.now());
        TaskOccurrence saved = taskOccurrenceRepository.save(occurrence);
        log.info("Task occurrence id {} completed by user id {}", occurrenceId, userId);

        return taskMapper.toTaskResponse(saved);
    }

    @Transactional
    public TaskResponse moveTaskToTomorrow(Long userId, Long occurrenceId) {
        User user = getUserOrThrow(userId);
        LocalDate userToday = LocalDate.now(ZoneId.of(user.getTimezone()));
        LocalDate tomorrow = userToday.plusDays(1);
        return moveTaskToDate(userId, occurrenceId, tomorrow);
    }

    @Transactional
    public TaskResponse moveTaskToDate(Long userId, Long occurrenceId, LocalDate targetDate) {
        User user = getUserOrThrow(userId);
        TaskOccurrence occurrence = taskOccurrenceRepository.findByIdAndUserId(occurrenceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task occurrence not found with id: " + occurrenceId));

        if (occurrence.getStatus() == TaskStatus.COMPLETED) {
            throw new InvalidTaskOperationException("Completed tasks cannot be moved unless restored.");
        }

        LocalDate sourceDate = occurrence.getOccurrenceDate();
        if (sourceDate.equals(targetDate)) {
            return taskMapper.toTaskResponse(occurrence);
        }

        // Preserve original occurrence state as MOVED for historical record
        occurrence.setStatus(TaskStatus.MOVED);
        taskOccurrenceRepository.save(occurrence);

        // Record task movement history
        TaskMovementHistory history = TaskMovementHistory.builder()
                .user(user)
                .taskOccurrence(occurrence)
                .sourceDate(sourceDate)
                .targetDate(targetDate)
                .build();
        movementHistoryRepository.save(history);

        // Create new occurrence for target date
        Instant dueDateTime = computeDueDateTime(targetDate, occurrence.getDueTime(), user.getTimezone());
        Instant reminderScheduledAt = calculateReminderScheduledAt(dueDateTime, occurrence.getReminderOption(), occurrence.getCustomReminderMinutes());

        TaskOccurrence targetOccurrence = TaskOccurrence.builder()
                .taskDefinition(occurrence.getTaskDefinition())
                .user(user)
                .title(occurrence.getTitle())
                .description(occurrence.getDescription())
                .priority(occurrence.getPriority())
                .occurrenceDate(targetDate)
                .dueTime(occurrence.getDueTime())
                .dueDateTime(dueDateTime)
                .reminderOption(occurrence.getReminderOption())
                .customReminderMinutes(occurrence.getCustomReminderMinutes())
                .reminderScheduledAt(reminderScheduledAt)
                .status(TaskStatus.PENDING)
                .build();

        TaskOccurrence savedTargetOccurrence = taskOccurrenceRepository.save(targetOccurrence);
        log.info("Moved task occurrence id {} from {} to target date {}. New occurrence id {}",
                occurrenceId, sourceDate, targetDate, savedTargetOccurrence.getId());

        return taskMapper.toTaskResponse(savedTargetOccurrence);
    }

    @Transactional
    public void materializeOccurrencesForUserAndDate(User user, LocalDate date) {
        List<TaskDefinition> activeDefinitions = taskDefinitionRepository
                .findByUserIdAndTaskTypeAndStartDateLessThanEqual(user.getId(), TaskType.DAILY_RECURRING, date);

        for (TaskDefinition def : activeDefinitions) {
            if (def.isLocked()) continue;

            boolean exists = taskOccurrenceRepository
                    .existsByUserIdAndTaskDefinitionIdAndOccurrenceDate(user.getId(), def.getId(), date);

            if (!exists) {
                Instant dueDateTime = computeDueDateTime(date, def.getDueTime(), user.getTimezone());
                Instant reminderScheduledAt = calculateReminderScheduledAt(dueDateTime, def.getReminderOption(), def.getCustomReminderMinutes());

                TaskOccurrence occurrence = TaskOccurrence.builder()
                        .taskDefinition(def)
                        .user(user)
                        .title(def.getTitle())
                        .description(def.getDescription())
                        .priority(def.getPriority())
                        .occurrenceDate(date)
                        .dueTime(def.getDueTime())
                        .dueDateTime(dueDateTime)
                        .reminderOption(def.getReminderOption())
                        .customReminderMinutes(def.getCustomReminderMinutes())
                        .reminderScheduledAt(reminderScheduledAt)
                        .status(TaskStatus.PENDING)
                        .build();

                taskOccurrenceRepository.save(occurrence);
                log.info("Materialized daily occurrence for def id {} on date {} for user {}", def.getId(), date, user.getId());
            }
        }
    }

    public Instant calculateReminderScheduledAt(Instant dueDateTime, ReminderOption option, Integer customMinutes) {
        if (option == null || option == ReminderOption.NONE) {
            return null;
        }
        int minutesToSubtract;
        if (option == ReminderOption.CUSTOM) {
            minutesToSubtract = (customMinutes != null && customMinutes > 0) ? customMinutes : 0;
        } else {
            minutesToSubtract = option.getDefaultMinutes();
        }
        if (minutesToSubtract <= 0) {
            return null;
        }
        return dueDateTime.minus(Duration.ofMinutes(minutesToSubtract));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Instant computeDueDateTime(LocalDate date, LocalTime time, String timezoneStr) {
        String normTz = com.application.taskmanager.user.model.AppTimezone.normalize(timezoneStr);
        ZoneId zoneId = ZoneId.of(normTz);
        return LocalDateTime.of(date, time).atZone(zoneId).toInstant();
    }
}
