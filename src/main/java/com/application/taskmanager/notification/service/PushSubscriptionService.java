package com.application.taskmanager.notification.service;

import com.application.taskmanager.exception.ResourceNotFoundException;
import com.application.taskmanager.notification.dto.PushSubscriptionRequest;
import com.application.taskmanager.notification.dto.PushSubscriptionResponse;
import com.application.taskmanager.notification.entity.PushSubscription;
import com.application.taskmanager.notification.repository.PushSubscriptionRepository;
import com.application.taskmanager.user.entity.User;
import com.application.taskmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;

    @Value("${app.vapid.public-key:BJIwL4mA3CEFwx50mAvSKHCxxIrjjLsfJZu2F1Of0r446101Q2kSB7Wm-pJK91i3QxPuPintSJ3vnS5XWfxf9fk}")
    private String vapidPublicKey;

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    @Transactional
    public PushSubscriptionResponse saveSubscription(Long userId, PushSubscriptionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Optional<PushSubscription> existing = pushSubscriptionRepository.findByEndpoint(request.getEndpoint());

        PushSubscription subscription;
        if (existing.isPresent()) {
            subscription = existing.get();
            subscription.setUser(user);
            subscription.setP256dhKey(request.getP256dhKey());
            subscription.setAuthKey(request.getAuthKey());
            subscription.setActive(true);
        } else {
            subscription = PushSubscription.builder()
                    .user(user)
                    .endpoint(request.getEndpoint())
                    .p256dhKey(request.getP256dhKey())
                    .authKey(request.getAuthKey())
                    .active(true)
                    .build();
        }

        PushSubscription saved = pushSubscriptionRepository.save(subscription);
        log.info("Saved active PushSubscription id {} for userId {}", saved.getId(), userId);

        return PushSubscriptionResponse.builder()
                .id(saved.getId())
                .userId(userId)
                .endpoint(saved.getEndpoint())
                .active(saved.isActive())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public void unsubscribe(Long userId, String endpoint) {
        pushSubscriptionRepository.deleteByEndpointAndUserId(endpoint, userId);
        log.info("Removed PushSubscription for userId {} and endpoint {}", userId, endpoint);
    }
}
