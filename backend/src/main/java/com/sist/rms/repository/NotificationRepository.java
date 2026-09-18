package com.sist.rms.repository;

import com.sist.rms.entity.Notification;
import com.sist.rms.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByCreatedAtDesc();

    List<Notification> findByDeveloperIdOrderByCreatedAtDesc(Long developerId);

    long countByReadFalse();

    /** Used when regenerating alerts, so the same warning is not stored twice. */
    boolean existsByTypeAndMessage(NotificationType type, String message);
}
