package com.application.taskmanager.notification.service;

import com.application.taskmanager.notification.entity.PushSubscription;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Security;

@Service
@Slf4j
public class WebPushService {

    @Value("${app.vapid.public-key:BJIwL4mA3CEFwx50mAvSKHCxxIrjjLsfJZu2F1Of0r446101Q2kSB7Wm-pJK91i3QxPuPintSJ3vnS5XWfxf9fk}")
    private String vapidPublicKey;

    @Value("${app.vapid.private-key:hOeFEoSpCgryCJH_QYCO_BdV5ZID_lzsvWCarwBoPl8}")
    private String vapidPrivateKey;

    @Value("${app.vapid.subject:mailto:ashrithbalajigudla@gmail.com}")
    private String vapidSubject;

    private PushService pushService;

    @PostConstruct
    public void init() {
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
            log.info("[WEB-PUSH] Successfully initialized PushService with RFC 8291 payload encryption and VAPID keypair");
        } catch (Exception e) {
            log.error("[WEB-PUSH] Failed to initialize PushService: {}", e.getMessage(), e);
        }
    }

    public boolean sendPushNotification(PushSubscription subscription, String title, String body, String url) {
        if (!subscription.isActive() || subscription.getEndpoint() == null || pushService == null) {
            log.warn("[WEB-PUSH] Skipped: active={}, endpoint={}, serviceInitialized={}",
                    subscription.isActive(), subscription.getEndpoint() != null, pushService != null);
            return false;
        }

        try {
            String payloadJson = String.format(
                    "{\"title\":\"%s\",\"body\":\"%s\",\"url\":\"%s\",\"icon\":\"/pwa-192x192.png\"}",
                    escapeJson(title), escapeJson(body), escapeJson(url != null ? url : "/dashboard")
            );

            Notification notification = new Notification(
                    subscription.getEndpoint(),
                    subscription.getP256dhKey(),
                    subscription.getAuthKey(),
                    payloadJson
            );

            org.apache.http.HttpResponse response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 201 || statusCode == 200 || statusCode == 202) {
                log.info("[WEB-PUSH] Successfully delivered RFC 8291 encrypted Web Push notification (HTTP {}) to endpoint {}", statusCode, subscription.getEndpoint());
                return true;
            } else if (statusCode == 404 || statusCode == 410) {
                log.warn("[WEB-PUSH] Push subscription expired or invalid (HTTP {}). Deactivating endpoint {}", statusCode, subscription.getEndpoint());
                subscription.setActive(false);
                return false;
            } else {
                log.warn("[WEB-PUSH] Push delivery endpoint returned HTTP {}", statusCode);
                return false;
            }
        } catch (Exception e) {
            log.error("[WEB-PUSH] Failed to send Web Push notification to endpoint {}: {}", subscription.getEndpoint(), e.getMessage(), e);
            return false;
        }
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
