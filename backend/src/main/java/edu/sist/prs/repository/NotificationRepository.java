package edu.sist.prs.repository;

import edu.sist.prs.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<NotificationEntity> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, Boolean isRead);
    Page<NotificationEntity> findByUserId(Long userId, Pageable pageable);
    Page<NotificationEntity> findByUserIdAndIsRead(Long userId, Boolean isRead, Pageable pageable);
    long countByUserIdAndIsReadFalse(Long userId);
}
