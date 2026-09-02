package com.application.taskmanager.notification.template;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TaskReminderEmailTemplate {

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    public String buildTaskReminderHtml(String userName, String taskTitle, String description, String priority, String dueTimeStr, String remainingTimeStr) {
        String priorityColor = "HIGH".equalsIgnoreCase(priority) ? "#dc3545" : ("MEDIUM".equalsIgnoreCase(priority) ? "#ffc107" : "#28a745");

        return "<html><body style='font-family: Arial, sans-serif; background-color: #f4f6f8; margin: 0; padding: 20px;'>" +
                "<table width='100%' border='0' cellspacing='0' cellpadding='0' style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05);'>" +
                "<tr style='background-color: #007bff; color: #ffffff;'><td style='padding: 20px; text-align: center;'>" +
                "<h2 style='margin: 0;'>⏰ Task Reminder</h2>" +
                "</td></tr>" +
                "<tr><td style='padding: 25px;'>" +
                "<p style='font-size: 16px; color: #333;'>Hello <strong>" + escapeHtml(userName) + "</strong>,</p>" +
                "<p style='font-size: 15px; color: #555;'>You have an upcoming task scheduled for <strong>" + escapeHtml(remainingTimeStr) + "</strong>:</p>" +
                "<div style='border-left: 4px solid " + priorityColor + "; background-color: #f8f9fa; padding: 15px; margin: 20px 0; border-radius: 4px;'>" +
                "<h3 style='margin: 0 0 8px 0; color: #222;'>" + escapeHtml(taskTitle) + "</h3>" +
                (description != null && !description.isBlank() ? "<p style='margin: 0 0 8px 0; color: #666; font-size: 14px;'>" + escapeHtml(description) + "</p>" : "") +
                "<p style='margin: 4px 0; color: #444; font-size: 14px;'><strong>Priority:</strong> <span style='color: " + priorityColor + "; font-weight: bold;'>" + escapeHtml(priority) + "</span></p>" +
                "<p style='margin: 4px 0; color: #444; font-size: 14px;'><strong>Due Time:</strong> " + escapeHtml(dueTimeStr) + "</p>" +
                "</div>" +
                "<div style='text-align: center; margin-top: 25px;'>" +
                "<a href='" + frontendBaseUrl + "' style='background-color: #007bff; color: #ffffff; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;'>View Task</a>" +
                "</div>" +
                "</td></tr>" +
                "<tr><td style='background-color: #f8f9fa; padding: 15px; text-align: center; color: #888; font-size: 12px; border-top: 1px solid #eeeeee;'>" +
                "Task Reminder Service &copy; 2026. All rights reserved." +
                "</td></tr>" +
                "</table></body></html>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
