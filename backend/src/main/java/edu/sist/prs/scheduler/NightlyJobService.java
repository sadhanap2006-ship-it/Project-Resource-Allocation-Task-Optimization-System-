package edu.sist.prs.scheduler;

import edu.sist.prs.entity.ConflictEntity;
import edu.sist.prs.service.ConflictDetectionService;
import edu.sist.prs.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class NightlyJobService {

    private static final Logger log = LoggerFactory.getLogger(NightlyJobService.class);

    private final ConflictDetectionService conflictDetectionService;
    private final NotificationService notificationService;

    public NightlyJobService(
            ConflictDetectionService conflictDetectionService,
            NotificationService notificationService
    ) {
        this.conflictDetectionService = conflictDetectionService;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void runNightlyMaintenance() {
        LocalDateTime start = LocalDateTime.now();
        log.info("Starting nightly maintenance job at {}", start);

        try {
            List<ConflictEntity> conflicts = conflictDetectionService.runFullScan();
            log.info("Nightly conflict detection scan completed with {} detected/active conflicts", conflicts.size());
        } catch (Exception e) {
            log.error("Nightly conflict scan failed", e);
        }

        LocalDateTime end = LocalDateTime.now();
        log.info("Nightly maintenance job completed at {}", end);
    }
}
