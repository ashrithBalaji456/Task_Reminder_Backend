package com.application.taskmanager.task.service;

import com.application.taskmanager.recurring.dto.RecurringTaskResponse;
import com.application.taskmanager.task.dto.TaskResponse;
import com.application.taskmanager.task.entity.TaskDefinition;
import com.application.taskmanager.task.entity.TaskOccurrence;
import com.application.taskmanager.task.entity.TaskType;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskResponse toTaskResponse(TaskOccurrence occurrence) {
        if (occurrence == null) return null;

        TaskDefinition def = occurrence.getTaskDefinition();
        boolean isRecurring = def != null && def.getTaskType() == TaskType.DAILY_RECURRING;

        return TaskResponse.builder()
                .id(occurrence.getId())
                .taskDefinitionId(def != null ? def.getId() : null)
                .title(occurrence.getTitle())
                .description(occurrence.getDescription())
                .priority(occurrence.getPriority())
                .taskType(def != null ? def.getTaskType() : TaskType.ONE_TIME)
                .recurrenceType(def != null ? def.getRecurrenceType() : com.application.taskmanager.task.entity.RecurrenceType.NONE)
                .dueDate(occurrence.getOccurrenceDate())
                .dueTime(occurrence.getDueTime())
                .dueDateTime(occurrence.getDueDateTime())
                .reminderOption(occurrence.getReminderOption())
                .customReminderMinutes(occurrence.getCustomReminderMinutes())
                .reminderScheduledAt(occurrence.getReminderScheduledAt())
                .status(occurrence.getStatus())
                .recurring(isRecurring)
                .completedAt(occurrence.getCompletedAt())
                .createdAt(occurrence.getCreatedAt())
                .updatedAt(occurrence.getUpdatedAt())
                .build();
    }

    public RecurringTaskResponse toRecurringTaskResponse(TaskDefinition definition) {
        if (definition == null) return null;

        return RecurringTaskResponse.builder()
                .id(definition.getId())
                .title(definition.getTitle())
                .description(definition.getDescription())
                .priority(definition.getPriority())
                .taskType(definition.getTaskType())
                .recurrenceType(definition.getRecurrenceType())
                .startDate(definition.getStartDate())
                .dueTime(definition.getDueTime())
                .locked(definition.isLocked())
                .createdAt(definition.getCreatedAt())
                .updatedAt(definition.getUpdatedAt())
                .build();
    }
}
