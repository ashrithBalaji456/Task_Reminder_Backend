package com.application.taskmanager.reminder.repository;

import com.application.taskmanager.reminder.entity.ReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    boolean existsByTaskOccurrenceIdAndReminderType(Long taskOccurrenceId, String reminderType);

    List<ReminderLog> findByUserId(Long userId);
}
