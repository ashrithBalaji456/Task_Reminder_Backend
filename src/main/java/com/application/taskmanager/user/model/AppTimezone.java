package com.application.taskmanager.user.model;

import lombok.Getter;

@Getter
public enum AppTimezone {
    UTC("UTC", "UTC (Coordinated Universal Time)"),
    ASIA_KOLKATA("Asia/Kolkata", "Asia/Kolkata (IST - India Standard Time)"),
    ASIA_CALCUTTA("Asia/Calcutta", "Asia/Calcutta (IST - India Standard Time)"),
    AMERICA_NEW_YORK("America/New_York", "America/New_York (EST/EDT - Eastern Time)"),
    AMERICA_CHICAGO("America/Chicago", "America/Chicago (CST/CDT - Central Time)"),
    AMERICA_DENVER("America/Denver", "America/Denver (MST/MDT - Mountain Time)"),
    AMERICA_LOS_ANGELES("America/Los_Angeles", "America/Los_Angeles (PST/PDT - Pacific Time)"),
    EUROPE_LONDON("Europe/London", "Europe/London (GMT/BST - UK Time)"),
    EUROPE_PARIS("Europe/Paris", "Europe/Paris (CET/CEST - Central Europe)"),
    EUROPE_BERLIN("Europe/Berlin", "Europe/Berlin (CET/CEST - Germany)"),
    ASIA_TOKYO("Asia/Tokyo", "Asia/Tokyo (JST - Japan Standard Time)"),
    ASIA_SHANGHAI("Asia/Shanghai", "Asia/Shanghai (CST - China Standard Time)"),
    ASIA_DUBAI("Asia/Dubai", "Asia/Dubai (GST - Gulf Standard Time)"),
    ASIA_SINGAPORE("Asia/Singapore", "Asia/Singapore (SGT - Singapore Time)"),
    AUSTRALIA_SYDNEY("Australia/Sydney", "Australia/Sydney (AEST/AEDT - Sydney)"),
    PACIFIC_AUCKLAND("Pacific/Auckland", "Pacific/Auckland (NZST/NZDT - New Zealand)");

    private final String zoneId;
    private final String displayName;

    AppTimezone(String zoneId, String displayName) {
        this.zoneId = zoneId;
        this.displayName = displayName;
    }

    public static String normalize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return UTC.getZoneId();
        }
        String clean = input.trim();
        if (clean.contains(" ")) {
            clean = clean.split(" ")[0];
        }
        for (AppTimezone tz : values()) {
            if (tz.name().equalsIgnoreCase(clean) || tz.getZoneId().equalsIgnoreCase(clean)) {
                return tz.getZoneId();
            }
        }
        try {
            java.time.ZoneId.of(clean);
            return clean;
        } catch (Exception e) {
            return UTC.getZoneId();
        }
    }
}
