package com.application.taskmanager.notification.service;

import com.application.taskmanager.notification.entity.PushSubscription;
import com.application.taskmanager.notification.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Urgency;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Security;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebPushService {

    private final PushSubscriptionRepository pushSubscriptionRepository;

    @Value("${app.vapid.public-key:BJIwL4mA3CEFwx50mAvSKHCxxIrjjLsfJZu2F1Of0r446101Q2kSB7Wm-pJK91i3QxPuPintSJ3vnS5XWfxf9fk}")
    private String vapidPublicKey;

    @Value("${app.vapid.private-key:hOeFEoSpCgryCJH_QYCO_lzsvWCarwBoPl8}")
    private String vapidPrivateKey;

    @Value("${app.vapid.subject:mailto:ashrithbalajigudla@gmail.com}")
    private String vapidSubject;

    private PushService pushService;

    @PostConstruct
    public void init() {
        initPushService();
    }

    private synchronized PushService initPushService() {
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
            log.info("[WEB-PUSH] Successfully initialized PushService with RFC 8291 payload encryption and VAPID keypair");
        } catch (Exception e) {
            log.error("[WEB-PUSH] Failed to initialize PushService: {}", e.getMessage(), e);
        }
        return pushService;
    }

    public boolean sendPushNotification(PushSubscription subscription, String title, String body, String url) {
        return sendPushNotification(subscription, title, body, url, null);
    }

    public boolean sendPushNotification(PushSubscription subscription, String title, String body, String url, Long taskId) {
        if (!subscription.isActive() || subscription.getEndpoint() == null) {
            log.warn("[WEB-PUSH] Skipped: active={}, endpoint={}",
                    subscription.isActive(), subscription.getEndpoint() != null);
            return false;
        }

        PushService service = pushService != null ? pushService : initPushService();
        if (service == null) {
            log.error("[WEB-PUSH] PushService unavailable");
            return false;
        }

        try {
            String payloadJson = String.format(
                    "{\"title\":\"%s\",\"body\":\"%s\",\"url\":\"%s\",\"icon\":\"/pwa-192x192.png\",\"taskId\":%s}",
                    escapeJson(title), escapeJson(body), escapeJson(url != null ? url : "/dashboard"),
                    taskId != null ? taskId.toString() : "null"
            );

            Notification notification = new Notification(
                    subscription.getEndpoint(),
                    subscription.getP256dhKey(),
                    subscription.getAuthKey(),
                    payloadJson
            );

            org.apache.http.HttpResponse response = service.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 201 || statusCode == 200 || statusCode == 202) {
                log.info("[WEB-PUSH] Successfully delivered RFC 8291 encrypted Web Push notification (HTTP {}) to endpoint {}", statusCode, subscription.getEndpoint());
                return true;
            } else if (statusCode == 404 || statusCode == 410) {
                log.warn("[WEB-PUSH] Push subscription expired or invalid (HTTP {}). Deactivating endpoint {}", statusCode, subscription.getEndpoint());
                subscription.setActive(false);
                try {
                    pushSubscriptionRepository.save(subscription);
                } catch (Exception ex) {
                    log.error("[WEB-PUSH] Failed to persist deactivated subscription: {}", ex.getMessage());
                }
                return false;
            } else {
                log.warn("[WEB-PUSH] Push delivery endpoint returned HTTP {}", statusCode);
                return false;
            }
        } catch (Exception e) {
            log.error("[WEB-PUSH] Failed to send Web Push notification to endpoint {}: {}", subscription.getEndpoint(), e.getMessage(), e);
            pushService = null;
            return false;
        }
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
