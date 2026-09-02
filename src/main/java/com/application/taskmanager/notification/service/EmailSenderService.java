package com.application.taskmanager.notification.service;

import com.application.taskmanager.notification.client.BrevoEmailClient;
import com.application.taskmanager.notification.client.BrevoEmailClient.InlineAttachment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSenderService {

    private final BrevoEmailClient brevoEmailClient;

    public String sendEmail(String recipientEmail, String recipientName, String subject, String htmlContent) {
        return brevoEmailClient.sendEmail(recipientEmail, recipientName, subject, htmlContent, null);
    }

    public String sendEmailWithAttachments(String recipientEmail, String recipientName, String subject, String htmlContent, List<InlineAttachment> attachments) {
        return brevoEmailClient.sendEmail(recipientEmail, recipientName, subject, htmlContent, attachments);
    }
}
