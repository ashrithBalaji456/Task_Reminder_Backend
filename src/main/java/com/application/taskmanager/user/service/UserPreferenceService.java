package com.application.taskmanager.user.service;

import com.application.taskmanager.exception.InvalidTaskOperationException;
import com.application.taskmanager.exception.ResourceNotFoundException;
import com.application.taskmanager.user.dto.UserEmailPreferenceRequest;
import com.application.taskmanager.user.dto.UserEmailPreferenceResponse;
import com.application.taskmanager.user.entity.User;
import com.application.taskmanager.user.entity.UserEmailPreference;
import com.application.taskmanager.user.repository.UserEmailPreferenceRepository;
import com.application.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserEmailPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserEmailPreferenceResponse getUserPreferences(Long userId) {
        User user = getUserOrThrow(userId);
        UserEmailPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(user));

        return toResponse(preference);
    }

    @Transactional
    public UserEmailPreferenceResponse updateUserPreferences(Long userId, UserEmailPreferenceRequest request) {
        User user = getUserOrThrow(userId);

        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            String normalizedTz = com.application.taskmanager.user.model.AppTimezone.normalize(request.getTimezone());
            user.setTimezone(normalizedTz);
            userRepository.save(user);
        }

        UserEmailPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(user));

        preference.setTaskReminderEnabled(request.getTaskReminderEnabled());
        if (request.getPushNotificationEnabled() != null) {
            preference.setPushNotificationEnabled(request.getPushNotificationEnabled());
        }
        preference.setWeeklyReportEnabled(request.getWeeklyReportEnabled());
        preference.setMonthlyReportEnabled(request.getMonthlyReportEnabled());

        if (request.getPreferredWeeklyReportDay() != null && !request.getPreferredWeeklyReportDay().isBlank()) {
            preference.setPreferredWeeklyReportDay(request.getPreferredWeeklyReportDay().toUpperCase());
        }

        if (request.getPreferredWeeklyReportTime() != null) {
            preference.setPreferredWeeklyReportTime(request.getPreferredWeeklyReportTime());
        }

        UserEmailPreference updated = preferenceRepository.save(preference);
        return toResponse(updated);
    }

    private UserEmailPreference createDefaultPreference(User user) {
        UserEmailPreference pref = UserEmailPreference.builder()
                .user(user)
                .taskReminderEnabled(true)
                .pushNotificationEnabled(true)
                .weeklyReportEnabled(true)
                .monthlyReportEnabled(true)
                .preferredWeeklyReportDay("SUNDAY")
                .preferredWeeklyReportTime(java.time.LocalTime.of(18, 0))
                .build();
        return preferenceRepository.save(pref);
    }

    private UserEmailPreferenceResponse toResponse(UserEmailPreference pref) {
        return UserEmailPreferenceResponse.builder()
                .id(pref.getId())
                .userId(pref.getUser().getId())
                .taskReminderEnabled(pref.isTaskReminderEnabled())
                .pushNotificationEnabled(pref.isPushNotificationEnabled())
                .weeklyReportEnabled(pref.isWeeklyReportEnabled())
                .monthlyReportEnabled(pref.isMonthlyReportEnabled())
                .preferredWeeklyReportDay(pref.getPreferredWeeklyReportDay())
                .preferredWeeklyReportTime(pref.getPreferredWeeklyReportTime())
                .timezone(pref.getUser().getTimezone())
                .updatedAt(pref.getUpdatedAt())
                .build();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
