package edu.sist.prs.service;

import edu.sist.prs.entity.ConflictSeverity;
import edu.sist.prs.entity.NotificationEntity;
import edu.sist.prs.entity.UserEntity;
import edu.sist.prs.exception.ApiException;
import edu.sist.prs.repository.NotificationRepository;
import edu.sist.prs.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public NotificationEntity createNotification(
            Long userId,
            Long projectId,
            Long conflictId,
            ConflictSeverity severity,
            String title,
            String message
    ) {
        NotificationEntity notification = new NotificationEntity(
                userId,
                projectId,
                conflictId,
                severity,
                title,
                message,
                false
        );
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationEntity> getUserNotifications(String userEmail, Boolean unreadOnly, int page, int size) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        int pageSize = Math.min(Math.max(1, size), 200);
        int pageIndex = Math.max(0, page);
        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize, Sort.by("id").descending());

        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationRepository.findByUserIdAndIsRead(user.getId(), false, pageRequest);
        }
        return notificationRepository.findByUserId(user.getId(), pageRequest);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markAsRead(String userEmail, Long notificationId) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Notification not found"));

        if (!notification.getUserId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Cannot modify another user's notification");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));

        List<NotificationEntity> unread = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(user.getId(), false);
        for (NotificationEntity n : unread) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
