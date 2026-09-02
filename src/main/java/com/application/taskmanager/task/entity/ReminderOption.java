package com.application.taskmanager.task.entity;

public enum ReminderOption {
    NONE(0),
    TEN_MINUTES(10),
    THIRTY_MINUTES(30),
    ONE_HOUR(60),
    TWO_HOURS(120),
    ONE_DAY(1440),
    CUSTOM(-1);

    private final int defaultMinutes;

    ReminderOption(int defaultMinutes) {
        this.defaultMinutes = defaultMinutes;
    }

    public int getDefaultMinutes() {
        return defaultMinutes;
    }
}
