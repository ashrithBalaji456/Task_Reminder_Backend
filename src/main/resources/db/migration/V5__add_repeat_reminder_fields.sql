-- Migration V5: Add repeat reminder frequency, stop condition, max count, and channel preferences

ALTER TABLE task_definitions
ADD COLUMN repeat_frequency_minutes INT DEFAULT NULL,
ADD COLUMN repeat_stop_condition VARCHAR(30) DEFAULT 'UNTIL_TASK_TIME',
ADD COLUMN max_reminder_count INT DEFAULT 5,
ADD COLUMN notify_by_email BOOLEAN DEFAULT TRUE,
ADD COLUMN notify_by_push BOOLEAN DEFAULT TRUE;

ALTER TABLE task_occurrences
ADD COLUMN repeat_frequency_minutes INT DEFAULT NULL,
ADD COLUMN repeat_stop_condition VARCHAR(30) DEFAULT 'UNTIL_TASK_TIME',
ADD COLUMN max_reminder_count INT DEFAULT 5,
ADD COLUMN reminder_sent_count INT DEFAULT 0,
ADD COLUMN notify_by_email BOOLEAN DEFAULT TRUE,
ADD COLUMN notify_by_push BOOLEAN DEFAULT TRUE;
