package edu.sist.prs.repository;

import edu.sist.prs.entity.MilestoneEntity;
import edu.sist.prs.entity.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MilestoneRepository extends JpaRepository<MilestoneEntity, Long> {
    List<MilestoneEntity> findByProjectId(Long projectId);
    List<MilestoneEntity> findByProjectIdOrderByStartDateAsc(Long projectId);
    List<MilestoneEntity> findByStatus(MilestoneStatus status);
}
