package com.application.taskmanager.notification.repository;

import com.application.taskmanager.notification.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByUserIdAndActiveTrue(Long userId);

    Optional<PushSubscription> findByEndpoint(String endpoint);

    void deleteByEndpointAndUserId(String endpoint, Long userId);
}
