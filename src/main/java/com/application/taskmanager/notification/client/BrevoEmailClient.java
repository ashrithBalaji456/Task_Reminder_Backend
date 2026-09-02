package com.application.taskmanager.notification.client;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class BrevoEmailClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public BrevoEmailClient(
            @Value("${app.brevo.api-key}") String apiKey,
            @Value("${app.brevo.sender-email}") String senderEmail,
            @Value("${app.brevo.sender-name:Task Reminder Service}") String senderName) {
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .build();
    }

    @Data
    @Builder
    public static class InlineAttachment {
        private String name;
        private byte[] contentBytes;
    }

    public String sendEmail(String recipientEmail, String recipientName, String subject, String htmlContent, List<InlineAttachment> attachments) {
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("placeholder")) {
            log.warn("Brevo API key is not configured. Simulating email dispatch to {}", recipientEmail);
            return "SIMULATED_MSG_ID_" + System.currentTimeMillis();
        }

        try {
            List<Map<String, String>> attachmentList = new ArrayList<>();
            if (attachments != null) {
                for (InlineAttachment att : attachments) {
                    if (att.getContentBytes() != null && att.getContentBytes().length > 0) {
                        String base64Content = Base64.getEncoder().encodeToString(att.getContentBytes());
                        attachmentList.add(Map.of(
                                "name", att.getName(),
                                "content", base64Content
                        ));
                    }
                }
            }

            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("sender", Map.of("name", senderName, "email", senderEmail));
            requestBody.put("to", List.of(Map.of("name", recipientName != null ? recipientName : "User", "email", recipientEmail)));
            requestBody.put("subject", subject);
            requestBody.put("htmlContent", htmlContent);

            if (!attachmentList.isEmpty()) {
                requestBody.put("attachment", attachmentList);
            }

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

            log.info("Successfully dispatched email via Brevo to {}. MessageId: {}", recipientEmail, messageId);
            return messageId;

        } catch (Exception ex) {
            log.error("Brevo API email dispatch error to {}: {}", recipientEmail, ex.getMessage());
            throw new RuntimeException("Brevo API Email dispatch failed: " + ex.getMessage(), ex);
        }
    }
}
