-- Add reset token columns to users table
ALTER TABLE users ADD COLUMN reset_token VARCHAR(255);
ALTER TABLE users ADD COLUMN reset_token_expiry TIMESTAMP WITH TIME ZONE;
