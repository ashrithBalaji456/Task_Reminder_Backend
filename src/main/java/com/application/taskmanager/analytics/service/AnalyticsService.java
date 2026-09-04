package com.application.taskmanager.analytics.service;

import com.application.taskmanager.analytics.dto.MonthlyAnalyticsResponse;
import com.application.taskmanager.analytics.dto.WeeklyAnalyticsResponse;
import com.application.taskmanager.exception.ResourceNotFoundException;
import com.application.taskmanager.task.entity.Priority;
import com.application.taskmanager.task.entity.TaskOccurrence;
import com.application.taskmanager.task.entity.TaskStatus;
import com.application.taskmanager.task.repository.TaskOccurrenceRepository;
import com.application.taskmanager.task.service.TaskService;
import com.application.taskmanager.user.entity.User;
import com.application.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final TaskOccurrenceRepository taskOccurrenceRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;

    @Transactional
    public WeeklyAnalyticsResponse getWeeklyAnalytics(Long userId, LocalDate referenceDate) {
        User user = getUserOrThrow(userId);
        ZoneId userZone = ZoneId.of(user.getTimezone());
        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now(userZone);

        // Previous completed week: Monday to Sunday of previous week
        LocalDate currentWeekMonday = refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate prevWeekStart = currentWeekMonday.minusWeeks(1);
        LocalDate prevWeekEnd = prevWeekStart.plusDays(6);

        // Materialize recurring occurrences for prev week dates
        for (LocalDate d = prevWeekStart; !d.isAfter(prevWeekEnd); d = d.plusDays(1)) {
            taskService.materializeOccurrencesForUserAndDate(user, d);
        }

        List<TaskOccurrence> weekOccurrences = taskOccurrenceRepository.findByUserIdAndOccurrenceDateBetween(userId, prevWeekStart, prevWeekEnd);

        // Calculations for target week
        WeeklyMetrics currentMetrics = computeMetrics(weekOccurrences, prevWeekStart, prevWeekEnd);

        // 2 Weeks Prior for comparison
        LocalDate priorWeekStart = prevWeekStart.minusWeeks(1);
        LocalDate priorWeekEnd = priorWeekStart.plusDays(6);
        for (LocalDate d = priorWeekStart; !d.isAfter(priorWeekEnd); d = d.plusDays(1)) {
            taskService.materializeOccurrencesForUserAndDate(user, d);
        }
        List<TaskOccurrence> priorOccurrences = taskOccurrenceRepository.findByUserIdAndOccurrenceDateBetween(userId, priorWeekStart, priorWeekEnd);
        WeeklyMetrics priorMetrics = computeMetrics(priorOccurrences, priorWeekStart, priorWeekEnd);

        double diff = Math.round((currentMetrics.completionRate - priorMetrics.completionRate) * 10.0) / 10.0;
        String compMsg;
        if (diff > 0) {
            compMsg = String.format("Improved by +%.1f percentage points compared to prior week.", diff);
        } else if (diff < 0) {
            compMsg = String.format("Decline of %.1f percentage points compared to prior week.", Math.abs(diff));
        } else {
            compMsg = "Maintained identical completion rate compared to prior week.";
        }

        return WeeklyAnalyticsResponse.builder()
                .startDate(prevWeekStart)
                .endDate(prevWeekEnd)
                .totalTasks(currentMetrics.totalTasks)
                .completedTasks(currentMetrics.completedTasks)
                .pendingTasks(currentMetrics.pendingTasks)
                .cancelledTasks(currentMetrics.cancelledTasks)
                .movedTasks(currentMetrics.movedTasks)
                .completionRate(currentMetrics.completionRate)
                .highPriorityCompleted(currentMetrics.highPriorityCompleted)
                .highPriorityPending(currentMetrics.highPriorityPending)
                .mostProductiveDay(currentMetrics.mostProductiveDay)
                .leastProductiveDay(currentMetrics.leastProductiveDay)
                .previousWeekCompletionRate(priorMetrics.completionRate)
                .completionRateDifference(diff)
                .comparisonMessage(compMsg)
                .dailyCompletedMap(currentMetrics.dailyCompletedMap)
                .build();
    }

    @Transactional
    public MonthlyAnalyticsResponse getMonthlyAnalytics(Long userId, LocalDate referenceDate) {
        User user = getUserOrThrow(userId);
        ZoneId userZone = ZoneId.of(user.getTimezone());
        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now(userZone);

        // Previous completed calendar month
        LocalDate firstDayOfCurrentMonth = refDate.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate targetMonthStart = firstDayOfCurrentMonth.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate targetMonthEnd = targetMonthStart.with(TemporalAdjusters.lastDayOfMonth());

        // Materialize recurring occurrences for target month
        for (LocalDate d = targetMonthStart; !d.isAfter(targetMonthEnd); d = d.plusDays(1)) {
            taskService.materializeOccurrencesForUserAndDate(user, d);
        }

        List<TaskOccurrence> monthOccurrences = taskOccurrenceRepository.findByUserIdAndOccurrenceDateBetween(userId, targetMonthStart, targetMonthEnd);

        MonthlyMetrics currentMetrics = computeMonthlyMetrics(monthOccurrences, targetMonthStart, targetMonthEnd);

        // Prior month for comparison
        LocalDate priorMonthStart = targetMonthStart.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate priorMonthEnd = priorMonthStart.with(TemporalAdjusters.lastDayOfMonth());
        for (LocalDate d = priorMonthStart; !d.isAfter(priorMonthEnd); d = d.plusDays(1)) {
            taskService.materializeOccurrencesForUserAndDate(user, d);
        }
        List<TaskOccurrence> priorOccurrences = taskOccurrenceRepository.findByUserIdAndOccurrenceDateBetween(userId, priorMonthStart, priorMonthEnd);
        MonthlyMetrics priorMetrics = computeMonthlyMetrics(priorOccurrences, priorMonthStart, priorMonthEnd);

        double diff = Math.round((currentMetrics.completionRate - priorMetrics.completionRate) * 10.0) / 10.0;
        String compMsg = diff >= 0
                ? String.format("Improved by +%.1f percentage points compared to previous month.", diff)
                : String.format("Decline of %.1f percentage points compared to previous month.", Math.abs(diff));

        String monthTitle = targetMonthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy"));

        return MonthlyAnalyticsResponse.builder()
                .monthName(monthTitle)
                .startDate(targetMonthStart)
                .endDate(targetMonthEnd)
                .totalTasks(currentMetrics.totalTasks)
                .completedTasks(currentMetrics.completedTasks)
                .pendingTasks(currentMetrics.pendingTasks)
                .cancelledTasks(currentMetrics.cancelledTasks)
                .movedTasks(currentMetrics.movedTasks)
                .completionRate(currentMetrics.completionRate)
                .highPriorityCompleted(currentMetrics.highPriorityCompleted)
                .highPriorityPending(currentMetrics.highPriorityPending)
                .mediumPriorityCompleted(currentMetrics.mediumPriorityCompleted)
                .mediumPriorityPending(currentMetrics.mediumPriorityPending)
                .lowPriorityCompleted(currentMetrics.lowPriorityCompleted)
                .lowPriorityPending(currentMetrics.lowPriorityPending)
                .mostProductiveDay(currentMetrics.mostProductiveDay)
                .bestWeek(currentMetrics.bestWeek)
                .previousMonthCompletionRate(priorMetrics.completionRate)
                .completionRateDifference(diff)
                .comparisonMessage(compMsg)
                .dailyCompletedMap(currentMetrics.dailyCompletedMap)
                .build();
    }

    private WeeklyMetrics computeMetrics(List<TaskOccurrence> list, LocalDate start, LocalDate end) {
        Map<LocalDate, Long> dailyMap = new TreeMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            dailyMap.put(d, 0L);
        }

        long total = list.stream().filter(o -> o.getStatus() != TaskStatus.CANCELLED).count();
        long completed = 0, pending = 0, cancelled = 0, moved = 0;
        long highComp = 0, highPend = 0;

        for (TaskOccurrence o : list) {
            if (o.getStatus() == TaskStatus.COMPLETED) {
                completed++;
                dailyMap.put(o.getOccurrenceDate(), dailyMap.getOrDefault(o.getOccurrenceDate(), 0L) + 1);
                if (o.getPriority() == Priority.HIGH) highComp++;
            } else if (o.getStatus() == TaskStatus.PENDING) {
                pending++;
                if (o.getPriority() == Priority.HIGH) highPend++;
            } else if (o.getStatus() == TaskStatus.CANCELLED) {
                cancelled++;
            } else if (o.getStatus() == TaskStatus.MOVED) {
                moved++;
            }
        }

        double rate = total > 0 ? Math.round(((double) completed / total * 100.0) * 10.0) / 10.0 : 0.0;

        LocalDate maxDay = dailyMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(start);

        LocalDate minDay = dailyMap.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(start);

        String mostProd = maxDay.format(DateTimeFormatter.ofPattern("EEEE (dd MMM)")) + " (" + dailyMap.get(maxDay) + " completed)";
        String leastProd = minDay.format(DateTimeFormatter.ofPattern("EEEE (dd MMM)")) + " (" + dailyMap.get(minDay) + " completed)";

        WeeklyMetrics m = new WeeklyMetrics();
        m.totalTasks = total;
        m.completedTasks = completed;
        m.pendingTasks = pending;
        m.cancelledTasks = cancelled;
        m.movedTasks = moved;
        m.completionRate = rate;
        m.highPriorityCompleted = highComp;
        m.highPriorityPending = highPend;
        m.mostProductiveDay = mostProd;
        m.leastProductiveDay = leastProd;
        m.dailyCompletedMap = dailyMap;
        return m;
    }

    private MonthlyMetrics computeMonthlyMetrics(List<TaskOccurrence> list, LocalDate start, LocalDate end) {
        WeeklyMetrics base = computeMetrics(list, start, end);

        long medComp = 0, medPend = 0, lowComp = 0, lowPend = 0;
        Map<Integer, Long> weekMap = new HashMap<>();

        for (TaskOccurrence o : list) {
            if (o.getPriority() == Priority.MEDIUM) {
                if (o.getStatus() == TaskStatus.COMPLETED) medComp++;
                else if (o.getStatus() == TaskStatus.PENDING) medPend++;
            } else if (o.getPriority() == Priority.LOW) {
                if (o.getStatus() == TaskStatus.COMPLETED) lowComp++;
                else if (o.getStatus() == TaskStatus.PENDING) lowPend++;
            }

            if (o.getStatus() == TaskStatus.COMPLETED) {
                int weekNo = (o.getOccurrenceDate().getDayOfMonth() - 1) / 7 + 1;
                weekMap.put(weekNo, weekMap.getOrDefault(weekNo, 0L) + 1);
            }
        }

        int bestWk = weekMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(1);

        long bestWkCount = weekMap.getOrDefault(bestWk, 0L);
        String bestWkStr = "Week " + bestWk + " (" + bestWkCount + " completed)";

        MonthlyMetrics mm = new MonthlyMetrics();
        mm.totalTasks = base.totalTasks;
        mm.completedTasks = base.completedTasks;
        mm.pendingTasks = base.pendingTasks;
        mm.cancelledTasks = base.cancelledTasks;
        mm.movedTasks = base.movedTasks;
        mm.completionRate = base.completionRate;
        mm.highPriorityCompleted = base.highPriorityCompleted;
        mm.highPriorityPending = base.highPriorityPending;
        mm.mediumPriorityCompleted = medComp;
        mm.mediumPriorityPending = medPend;
        mm.lowPriorityCompleted = lowComp;
        mm.lowPriorityPending = lowPend;
        mm.mostProductiveDay = base.mostProductiveDay;
        mm.bestWeek = bestWkStr;
        mm.dailyCompletedMap = base.dailyCompletedMap;
        return mm;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private static class WeeklyMetrics {
        long totalTasks, completedTasks, pendingTasks, cancelledTasks, movedTasks;
        double completionRate;
        long highPriorityCompleted, highPriorityPending;
        String mostProductiveDay, leastProductiveDay;
        Map<LocalDate, Long> dailyCompletedMap;
    }

    private static class MonthlyMetrics extends WeeklyMetrics {
        long mediumPriorityCompleted, mediumPriorityPending;
        long lowPriorityCompleted, lowPriorityPending;
        String bestWeek;
    }
}
