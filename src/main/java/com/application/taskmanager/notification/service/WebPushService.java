package com.application.taskmanager.notification.service;

import com.application.taskmanager.notification.entity.PushSubscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class WebPushService {

    @Value("${app.vapid.public-key:BJIwL4mA3CEFwx50mAvSKHCxxIrjjLsfJZu2F1Of0r446101Q2kSB7Wm-pJK91i3QxPuPintSJ3vnS5XWfxf9fk}")
    private String vapidPublicKey;

    @Value("${app.vapid.subject:mailto:ashrithbalajigudla@gmail.com}")
    private String vapidSubject;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean sendPushNotification(PushSubscription subscription, String title, String body, String url) {
        if (!subscription.isActive() || subscription.getEndpoint() == null) {
            return false;
        }

        try {
            String payloadJson = String.format(
                    "{\"title\":\"%s\",\"body\":\"%s\",\"url\":\"%s\",\"icon\":\"/pwa-192x192.png\"}",
                    escapeJson(title), escapeJson(body), escapeJson(url != null ? url : "/dashboard")
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(subscription.getEndpoint()))
                    .header("Content-Type", "application/json")
                    .header("TTL", "86400")
                    .header("Crypto-Key", "p256ecdsa=" + vapidPublicKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 201 || statusCode == 200 || statusCode == 202) {
                log.info("Successfully delivered Web Push notification to endpoint {}", subscription.getEndpoint());
                return true;
            } else if (statusCode == 404 || statusCode == 410) {
                log.warn("Push subscription expired or invalid (HTTP {}). Deactivating endpoint {}", statusCode, subscription.getEndpoint());
                subscription.setActive(false);
                return false;
            } else {
                log.warn("Push delivery endpoint returned HTTP {}: {}", statusCode, response.body());
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to send Web Push notification to endpoint {}: {}", subscription.getEndpoint(), e.getMessage());
            return false;
        }
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
