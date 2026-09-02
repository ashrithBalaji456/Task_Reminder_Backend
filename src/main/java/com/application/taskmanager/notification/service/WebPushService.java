package com.application.taskmanager.notification.service;

import com.application.taskmanager.notification.entity.PushSubscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class WebPushService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Dispatch Web Push Notification payload to a user subscription endpoint
     */
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
                    .header("TTL", "86400") // 24 hours
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                log.info("Successfully delivered Web Push notification to endpoint {}", subscription.getEndpoint());
                return true;
            } else if (response.statusCode() == 404 || response.statusCode() == 410) {
                log.warn("Push subscription expired or invalid (HTTP {}). Deactivating endpoint {}", response.statusCode(), subscription.getEndpoint());
                subscription.setActive(false);
                return false;
            } else {
                log.warn("Push delivery endpoint returned HTTP {}: {}", response.statusCode(), response.body());
                return false;
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
