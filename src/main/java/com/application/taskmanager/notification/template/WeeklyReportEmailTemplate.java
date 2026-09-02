package com.application.taskmanager.notification.template;

import com.application.taskmanager.analytics.dto.WeeklyAnalyticsResponse;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class WeeklyReportEmailTemplate {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public String buildWeeklyReportHtml(String userName, WeeklyAnalyticsResponse analytics) {
        String periodStr = analytics.getStartDate().format(DATE_FORMATTER) + " – " + analytics.getEndDate().format(DATE_FORMATTER);
        String compColor = analytics.getCompletionRateDifference() >= 0 ? "#28a745" : "#dc3545";

        return "<html><body style='font-family: Arial, sans-serif; background-color: #f4f6f8; margin: 0; padding: 20px;'>" +
                "<table width='100%' border='0' cellspacing='0' cellpadding='0' style='max-width: 650px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05);'>" +
                "<tr style='background-color: #28a745; color: #ffffff;'><td style='padding: 20px; text-align: center;'>" +
                "<h2 style='margin: 0;'>📊 Weekly Productivity Report</h2>" +
                "<p style='margin: 5px 0 0 0; opacity: 0.9; font-size: 14px;'>" + periodStr + "</p>" +
                "</td></tr>" +
                "<tr><td style='padding: 25px;'>" +
                "<p style='font-size: 16px; color: #333;'>Hello <strong>" + escapeHtml(userName) + "</strong>,</p>" +
                "<p style='font-size: 15px; color: #555;'>Here is your weekly productivity breakdown for the previous completed week:</p>" +

                "<div style='text-align: center; background-color: #f8f9fa; padding: 20px; border-radius: 6px; margin: 20px 0; border: 1px solid #e9ecef;'>" +
                "<span style='font-size: 14px; color: #666; text-transform: uppercase; font-weight: bold;'>Weekly Completion Rate</span>" +
                "<div style='font-size: 42px; font-weight: bold; color: #28a745; margin: 10px 0;'>" + analytics.getCompletionRate() + "%</div>" +
                "<div style='font-size: 14px; color: " + compColor + "; font-weight: bold;'>" + escapeHtml(analytics.getComparisonMessage()) + "</div>" +
                "</div>" +

                "<table width='100%' border='0' cellspacing='10' cellpadding='0' style='margin: 20px 0;'>" +
                "<tr>" +
                "<td width='25%' style='background-color: #e3f2fd; padding: 12px; border-radius: 6px; text-align: center;'><div style='font-size: 20px; font-weight: bold; color: #1976d2;'>" + analytics.getTotalTasks() + "</div><div style='font-size: 12px; color: #555;'>Total</div></td>" +
                "<td width='25%' style='background-color: #e8f5e9; padding: 12px; border-radius: 6px; text-align: center;'><div style='font-size: 20px; font-weight: bold; color: #388e3c;'>" + analytics.getCompletedTasks() + "</div><div style='font-size: 12px; color: #555;'>Completed</div></td>" +
                "<td width='25%' style='background-color: #fff3e0; padding: 12px; border-radius: 6px; text-align: center;'><div style='font-size: 20px; font-weight: bold; color: #f57c00;'>" + analytics.getPendingTasks() + "</div><div style='font-size: 12px; color: #555;'>Pending</div></td>" +
                "<td width='25%' style='background-color: #f3e5f5; padding: 12px; border-radius: 6px; text-align: center;'><div style='font-size: 20px; font-weight: bold; color: #7b1fa2;'>" + analytics.getMovedTasks() + "</div><div style='font-size: 12px; color: #555;'>Moved</div></td>" +
                "</tr></table>" +

                "<div style='margin: 25px 0; padding: 15px; background-color: #fafafa; border-radius: 6px; border-left: 4px solid #17a2b8;'>" +
                "<p style='margin: 5px 0; color: #333; font-size: 14px;'><strong>Most Productive Day:</strong> " + escapeHtml(analytics.getMostProductiveDay()) + "</p>" +
                "<p style='margin: 5px 0; color: #333; font-size: 14px;'><strong>Least Productive Day:</strong> " + escapeHtml(analytics.getLeastProductiveDay()) + "</p>" +
                "<p style='margin: 5px 0; color: #333; font-size: 14px;'><strong>High Priority Completed:</strong> " + analytics.getHighPriorityCompleted() + " (Pending: " + analytics.getHighPriorityPending() + ")</p>" +
                "</div>" +

                "<div style='text-align: center; margin-top: 25px;'>" +
                "<h4 style='color: #444; margin-bottom: 10px;'>Daily Tasks Completed Chart</h4>" +
                "<img src='cid:weekly_chart.png' alt='Daily Completion Chart' style='max-width: 100%; height: auto; border-radius: 6px; border: 1px solid #e0e0e0;' />" +
                "</div>" +

                "</td></tr>" +
                "<tr><td style='background-color: #f8f9fa; padding: 15px; text-align: center; color: #888; font-size: 12px; border-top: 1px solid #eeeeee;'>" +
                "Keep up the great work! Task Reminder Service &copy; 2026." +
                "</td></tr>" +
                "</table></body></html>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
