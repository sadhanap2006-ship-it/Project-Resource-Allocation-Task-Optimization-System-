package edu.sist.prs.repository;

import edu.sist.prs.entity.ConflictEntity;
import edu.sist.prs.entity.ConflictSeverity;
import edu.sist.prs.entity.ConflictStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConflictRepository extends JpaRepository<ConflictEntity, Long> {

    Optional<ConflictEntity> findByDedupKey(String dedupKey);

    List<ConflictEntity> findByProjectId(Long projectId);

    List<ConflictEntity> findByResourceId(Long resourceId);

    List<ConflictEntity> findByStatus(ConflictStatus status);

    List<ConflictEntity> findByStatusAndSeverity(ConflictStatus status, ConflictSeverity severity);

    List<ConflictEntity> findByResourceIdAndStatus(Long resourceId, ConflictStatus status);

    List<ConflictEntity> findByProjectIdAndStatus(Long projectId, ConflictStatus status);
}
