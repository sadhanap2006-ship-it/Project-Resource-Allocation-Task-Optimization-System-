package com.sist.rms.service;

import com.sist.rms.dto.BudgetResponse;
import com.sist.rms.dto.ConflictResponse;
import com.sist.rms.dto.NotificationResponse;
import com.sist.rms.dto.SkillGapResponse;
import com.sist.rms.entity.*;
import com.sist.rms.exception.ResourceNotFoundException;
import com.sist.rms.repository.DeveloperRepository;
import com.sist.rms.repository.MilestoneRepository;
import com.sist.rms.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * IN-APP NOTIFICATIONS
 *
 * refresh() re-scans the whole system and stores an alert for every problem
 * it finds:
 *
 *   OVER_ALLOCATION      a developer is booked beyond their availability
 *   LEAVE_CONFLICT       a developer is booked while on leave
 *   SKILL_GAP            a milestone is missing a required skill
 *   BUDGET_RISK          a project has used 75 % or more of its budget
 *   MILESTONE_DELAY      a milestone is marked DELAYED, or its end date has
 *                        passed while progress is below 100 %
 *   MILESTONE_DEADLINE   a milestone ends within the next 7 days
 *
 * The same message is never stored twice, so pressing Refresh repeatedly
 * does not fill the screen with duplicates.
 */
@Service
public class NotificationService {

    /** A milestone ending within this many days counts as "approaching". */
    public static final int DEADLINE_WARNING_DAYS = 7;

    private static final DateTimeFormatter NICE_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private final NotificationRepository notificationRepository;
    private final DeveloperRepository developerRepository;
    private final MilestoneRepository milestoneRepository;
    private final ConflictService conflictService;
    private final SkillGapService skillGapService;
    private final BudgetService budgetService;

    public NotificationService(NotificationRepository notificationRepository,
                               DeveloperRepository developerRepository,
                               MilestoneRepository milestoneRepository,
                               ConflictService conflictService,
                               SkillGapService skillGapService,
                               BudgetService budgetService) {
        this.notificationRepository = notificationRepository;
        this.developerRepository = developerRepository;
        this.milestoneRepository = milestoneRepository;
        this.conflictService = conflictService;
        this.skillGapService = skillGapService;
        this.budgetService = budgetService;
    }

    // -----------------------------------------------------------------
    //  Reading
    // -----------------------------------------------------------------

    public List<NotificationResponse> findAll() {
        List<NotificationResponse> responses = new ArrayList<>();
        for (Notification notification : notificationRepository.findAllByOrderByCreatedAtDesc()) {
            responses.add(toResponse(notification));
        }
        return responses;
    }

    public List<NotificationResponse> findForDeveloper(Long developerId) {
        List<NotificationResponse> responses = new ArrayList<>();
        for (Notification notification : notificationRepository.findByDeveloperIdOrderByCreatedAtDesc(developerId)) {
            responses.add(toResponse(notification));
        }
        return responses;
    }

    public long countUnread() {
        return notificationRepository.countByReadFalse();
    }

    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void deleteAll() {
        notificationRepository.deleteAll();
    }

    // -----------------------------------------------------------------
    //  Generating
    // -----------------------------------------------------------------

    /** Re-scans everything and stores any new alerts. Returns the full list. */
    @Transactional
    public List<NotificationResponse> refresh() {
        LocalDate today = LocalDate.now();

        // 1 + 2 : conflicts (over-allocation and leave)
        for (ConflictResponse conflict : conflictService.findAllConflicts()) {
            NotificationType type = "OVER_ALLOCATION".equals(conflict.getType())
                    ? NotificationType.OVER_ALLOCATION : NotificationType.LEAVE_CONFLICT;
            NotificationSeverity severity = "OVER_ALLOCATION".equals(conflict.getType())
                    ? NotificationSeverity.CRITICAL : NotificationSeverity.WARNING;
            createIfNew(type, severity, conflict.getMessage(),
                    "DEVELOPER", conflict.getDeveloperId(), conflict.getDeveloperId());
        }

        // 3 : skill gaps
        for (SkillGapResponse gap : skillGapService.findAllSkillGaps()) {
            createIfNew(NotificationType.SKILL_GAP, NotificationSeverity.WARNING,
                    gap.getMessage() + " " + String.join(" | ", gap.getSuggestions()),
                    "MILESTONE", gap.getMilestoneId(), null);
        }

        // 4 : budget risks
        for (BudgetResponse budget : budgetService.getAllBudgets()) {
            if ("CRITICAL".equals(budget.getWarningLevel())) {
                createIfNew(NotificationType.BUDGET_RISK, NotificationSeverity.CRITICAL,
                        budget.getMessage(), "PROJECT", budget.getProjectId(), null);
            } else if ("WARNING".equals(budget.getWarningLevel())) {
                createIfNew(NotificationType.BUDGET_RISK, NotificationSeverity.WARNING,
                        budget.getMessage(), "PROJECT", budget.getProjectId(), null);
            }
        }

        // 5 + 6 : milestone delays and approaching deadlines
        for (Milestone milestone : milestoneRepository.findAllByOrderByStartDateAsc()) {
            if (milestone.getStatus() == MilestoneStatus.COMPLETED) {
                continue;
            }
            boolean overdue = milestone.getEndDate().isBefore(today)
                    && milestone.getProgressPercentage() < 100;

            if (milestone.getStatus() == MilestoneStatus.DELAYED || overdue) {
                createIfNew(NotificationType.MILESTONE_DELAY, NotificationSeverity.CRITICAL,
                        "MILESTONE DELAY: " + milestone.getName() + " in "
                                + milestone.getProject().getName() + " was due on "
                                + milestone.getEndDate().format(NICE_DATE) + " and is only "
                                + milestone.getProgressPercentage() + "% complete.",
                        "MILESTONE", milestone.getId(), null);
            } else if (!milestone.getEndDate().isBefore(today)
                    && !milestone.getEndDate().isAfter(today.plusDays(DEADLINE_WARNING_DAYS))) {
                createIfNew(NotificationType.MILESTONE_DEADLINE, NotificationSeverity.INFO,
                        "DEADLINE APPROACHING: " + milestone.getName() + " in "
                                + milestone.getProject().getName() + " is due on "
                                + milestone.getEndDate().format(NICE_DATE) + " ("
                                + milestone.getProgressPercentage() + "% complete).",
                        "MILESTONE", milestone.getId(), null);
            }
        }

        return findAll();
    }

    /** Stores the alert unless an identical one is already stored. */
    @Transactional
    public void createIfNew(NotificationType type, NotificationSeverity severity, String message,
                            String entityType, Long entityId, Long developerId) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        String trimmed = message.length() > 500 ? message.substring(0, 500) : message;
        if (notificationRepository.existsByTypeAndMessage(type, trimmed)) {
            return;
        }
        Notification notification = new Notification();
        notification.setType(type);
        notification.setSeverity(severity);
        notification.setMessage(trimmed);
        notification.setRelatedEntityType(entityType);
        notification.setRelatedEntityId(entityId);
        notification.setRead(false);
        notification.setCreatedAt(java.time.LocalDateTime.now());
        if (developerId != null) {
            developerRepository.findById(developerId).ifPresent(notification::setDeveloper);
        }
        notificationRepository.save(notification);
    }

    public NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType().name());
        response.setSeverity(notification.getSeverity().name());
        response.setMessage(notification.getMessage());
        response.setRelatedEntityType(notification.getRelatedEntityType());
        response.setRelatedEntityId(notification.getRelatedEntityId());
        response.setDeveloperId(notification.getDeveloper() == null ? null : notification.getDeveloper().getId());
        response.setRead(notification.getRead());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }
}
