-- V4__create_push_subscriptions.sql
CREATE TABLE IF NOT EXISTS push_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    endpoint TEXT NOT NULL UNIQUE,
    p256dh_key TEXT NOT NULL,
    auth_key TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_push_sub_user_id ON push_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_push_sub_endpoint ON push_subscriptions(endpoint);

-- Add push_notification_enabled column to user_email_preferences if not exists
ALTER TABLE user_email_preferences
ADD COLUMN IF NOT EXISTS push_notification_enabled BOOLEAN NOT NULL DEFAULT true;
