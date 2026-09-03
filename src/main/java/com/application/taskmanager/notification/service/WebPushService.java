package com.application.taskmanager.notification.service;

import com.application.taskmanager.notification.entity.PushSubscription;
import com.application.taskmanager.notification.util.VapidJwtHelper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.time.Duration;

@Service
@Slf4j
public class WebPushService {

    @Value("${app.vapid.public-key:BJIwL4mA3CEFwx50mAvSKHCxxIrjjLsfJZu2F1Of0r446101Q2kSB7Wm-pJK91i3QxPuPintSJ3vnS5XWfxf9fk}")
    private String vapidPublicKey;

    @Value("${app.vapid.private-key:hOeFEoSpCgryCJH_QYCO_BdV5ZID_lzsvWCarwBoPl8}")
    private String vapidPrivateKey;

    @Value("${app.vapid.subject:mailto:ashrithbalajigudla@gmail.com}")
    private String vapidSubject;

    private PrivateKey parsedPrivateKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @PostConstruct
    public void init() {
        try {
            parsedPrivateKey = VapidJwtHelper.getPrivateKey(vapidPublicKey, vapidPrivateKey);
            log.info("Successfully initialized VAPID EC PrivateKey for Web Push signing");
        } catch (Exception e) {
            log.error("Failed to parse VAPID private key: {}", e.getMessage(), e);
        }
    }

    public boolean sendPushNotification(PushSubscription subscription, String title, String body, String url) {
        if (!subscription.isActive() || subscription.getEndpoint() == null || parsedPrivateKey == null) {
            log.warn("Web Push skipped: active={}, endpoint={}, keyPresent={}",
                    subscription.isActive(), subscription.getEndpoint() != null, parsedPrivateKey != null);
            return false;
        }

        try {
            String vapidToken = VapidJwtHelper.createVapidToken(subscription.getEndpoint(), vapidSubject, parsedPrivateKey);
            String authHeader = "vapid t=" + vapidToken + ", k=" + vapidPublicKey;

            String payloadJson = String.format(
                    "{\"title\":\"%s\",\"body\":\"%s\",\"url\":\"%s\",\"icon\":\"/pwa-192x192.png\"}",
                    escapeJson(title), escapeJson(body), escapeJson(url != null ? url : "/dashboard")
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(subscription.getEndpoint()))
                    .header("Content-Type", "application/json")
                    .header("TTL", "86400")
                    .header("Authorization", authHeader)
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 201 || statusCode == 200 || statusCode == 202) {
                log.info("Successfully delivered VAPID Web Push notification (HTTP {}) to endpoint {}", statusCode, subscription.getEndpoint());
                return true;
            } else if (statusCode == 404 || statusCode == 410) {
                log.warn("Push subscription expired or invalid (HTTP {}). Deactivating endpoint {}", statusCode, subscription.getEndpoint());
                subscription.setActive(false);
                return false;
            } else {
                log.warn("Push delivery endpoint returned HTTP {}: {}", statusCode, response.body());
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
