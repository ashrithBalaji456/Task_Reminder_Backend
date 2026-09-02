-- V2__email_and_analytics_schema.sql
-- Database Migration Script for Email Notifications, User Preferences, and Reminders

-- 1. User Email Preferences Table
CREATE TABLE IF NOT EXISTS user_email_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    task_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    weekly_report_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    monthly_report_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    preferred_weekly_report_day VARCHAR(20) NOT NULL DEFAULT 'SUNDAY',
    preferred_weekly_report_time TIME NOT NULL DEFAULT '18:00:00',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Task Reminder Fields on Task Definitions & Occurrences
ALTER TABLE task_definitions
ADD COLUMN IF NOT EXISTS reminder_option VARCHAR(30) NOT NULL DEFAULT 'NONE',
ADD COLUMN IF NOT EXISTS custom_reminder_minutes INT;

ALTER TABLE task_occurrences
ADD COLUMN IF NOT EXISTS reminder_option VARCHAR(30) NOT NULL DEFAULT 'NONE',
ADD COLUMN IF NOT EXISTS custom_reminder_minutes INT,
ADD COLUMN IF NOT EXISTS reminder_scheduled_at TIMESTAMP WITH TIME ZONE;

-- 3. Email Notifications Table
CREATE TABLE IF NOT EXISTS email_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    task_occurrence_id BIGINT REFERENCES task_occurrences(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL,
    period_identifier VARCHAR(50),
    scheduled_for TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    provider_message_id VARCHAR(100),
    sent_at TIMESTAMP WITH TIME ZONE,
    failure_reason TEXT,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_reminder_occurrence UNIQUE (task_occurrence_id, notification_type, scheduled_for),
    CONSTRAINT uq_report_period UNIQUE (user_id, notification_type, period_identifier)
);

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_email_notif_status_scheduled ON email_notifications(status, scheduled_for);
CREATE INDEX IF NOT EXISTS idx_email_notif_user_status ON email_notifications(user_id, status);
CREATE INDEX IF NOT EXISTS idx_task_occ_reminder ON task_occurrences(reminder_scheduled_at, status);
