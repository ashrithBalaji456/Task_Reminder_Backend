package com.application.taskmanager.user.repository;

import com.application.taskmanager.user.entity.UserEmailPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserEmailPreferenceRepository extends JpaRepository<UserEmailPreference, Long> {

    Optional<UserEmailPreference> findByUserId(Long userId);
}
