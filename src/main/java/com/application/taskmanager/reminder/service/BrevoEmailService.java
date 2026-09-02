package com.application.taskmanager.reminder.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BrevoEmailService {

    private final RestClient restClient;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public BrevoEmailService(
            @Value("${app.brevo.api-key}") String apiKey,
            @Value("${app.brevo.sender-email:noreply@taskreminder.com}") String senderEmail,
            @Value("${app.brevo.sender-name:Task Reminder Service}") String senderName) {
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .build();
    }

    public String sendReminderEmail(String recipientEmail, String recipientName, String taskTitle, String priority, String dueTimeStr) {
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("placeholder") || apiKey.contains("your-brevo-api-key")) {
            log.warn("Brevo API key is not configured. Simulating email dispatch to {}", recipientEmail);
            return "SIMULATED_MSG_ID_" + System.currentTimeMillis();
        }

        try {
            String subject = String.format("⏰ Task Reminder: %s [%s Priority]", taskTitle, priority);
            String htmlContent = String.format(
                    "<html><body>" +
                    "<h2>Hello %s,</h2>" +
                    "<p>This is a reminder for your upcoming task:</p>" +
                    "<div style='padding: 15px; background-color: #f4f6f8; border-left: 5px solid #007bff; margin: 10px 0;'>" +
                    "<h3>%s</h3>" +
                    "<p><strong>Priority:</strong> %s</p>" +
                    "<p><strong>Due Time:</strong> %s</p>" +
                    "</div>" +
                    "<p>Please log in to your Task Reminder app to complete or update this task.</p>" +
                    "<br><p>Best regards,<br>Task Reminder Team</p>" +
                    "</body></html>",
                    recipientName, taskTitle, priority, dueTimeStr
            );

            Map<String, Object> requestBody = Map.of(
                    "sender", Map.of("name", senderName, "email", senderEmail),
                    "to", List.of(Map.of("name", recipientName, "email", recipientEmail)),
                    "subject", subject,
                    "htmlContent", htmlContent
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            String messageId = response != null && response.containsKey("messageId")
                    ? response.get("messageId").toString()
                    : "BREVO_SENT_" + System.currentTimeMillis();

            log.info("Successfully sent Brevo reminder email to {}. MessageId: {}", recipientEmail, messageId);
            return messageId;

        } catch (Exception ex) {
            log.error("Failed to send Brevo email to {}: {}", recipientEmail, ex.getMessage(), ex);
            throw new RuntimeException("Brevo API Email dispatch failed: " + ex.getMessage(), ex);
        }
    }

    public String sendPasswordResetEmail(String recipientEmail, String recipientName, String resetToken) {
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("placeholder") || apiKey.contains("your-brevo-api-key")) {
            log.warn("Brevo API key is not configured. Simulating password reset email to {}", recipientEmail);
            return "SIMULATED_RESET_MSG_ID_" + System.currentTimeMillis();
        }

        try {
            String resetUrl = "http://localhost:3000/reset-password?token=" + resetToken + "&email=" + recipientEmail;
            String subject = "🔒 Reset Your Password - RemindMe 🌸";
            String htmlContent = String.format(
                    "<html><body style='font-family: Arial, sans-serif; color: #333; line-height: 1.6;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 16px; background-color: #ffffff;'>" +
                    "<h2 style='color: #ec4899;'>RemindMe Password Reset Request</h2>" +
                    "<p>Hello <strong>%s</strong>,</p>" +
                    "<p>We received a request to reset your password for your RemindMe account.</p>" +
                    "<p>Click the button below or copy your security reset token to set a new password:</p>" +
                    "<div style='text-align: center; margin: 25px 0;'>" +
                    "<a href='%s' style='background: linear-gradient(135deg, #ec4899 0%%, #a855f7 100%%); color: white; padding: 12px 25px; text-decoration: none; border-radius: 12px; font-weight: bold; display: inline-block;'>Reset Password</a>" +
                    "</div>" +
                    "<div style='padding: 12px; background-color: #fce7f3; border-radius: 10px; text-align: center; margin-bottom: 20px;'>" +
                    "<p style='margin: 0; font-size: 13px; color: #be185d;'><strong>Security Reset Token:</strong></p>" +
                    "<p style='margin: 5px 0 0 0; font-size: 16px; font-weight: bold; font-family: monospace; letter-spacing: 1px; color: #831843;'>%s</p>" +
                    "</div>" +
                    "<p style='font-size: 12px; color: #666;'>This reset token will expire in 15 minutes. If you did not request a password reset, you can safely ignore this email.</p>" +
                    "<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;' />" +
                    "<p style='font-size: 12px; color: #999;'>Best regards,<br>RemindMe Productivity Team</p>" +
                    "</div></body></html>",
                    recipientName, resetUrl, resetToken
            );

            Map<String, Object> requestBody = Map.of(
                    "sender", Map.of("name", senderName, "email", senderEmail),
                    "to", List.of(Map.of("name", recipientName, "email", recipientEmail)),
                    "subject", subject,
                    "htmlContent", htmlContent
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            String messageId = response != null && response.containsKey("messageId")
                    ? response.get("messageId").toString()
                    : "BREVO_SENT_" + System.currentTimeMillis();

            log.info("Successfully sent password reset email to {}. MessageId: {}", recipientEmail, messageId);
            return messageId;

        } catch (Exception ex) {
            log.error("Failed to send password reset email to {}: {}", recipientEmail, ex.getMessage(), ex);
            throw new RuntimeException("Brevo API Password Reset email dispatch failed: " + ex.getMessage(), ex);
        }
    }
}
