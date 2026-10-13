package edu.sist.prs.repository;

import edu.sist.prs.entity.MilestoneSkillRequirementEntity;
import edu.sist.prs.entity.MilestoneSkillRequirementId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MilestoneSkillRequirementRepository extends JpaRepository<MilestoneSkillRequirementEntity, MilestoneSkillRequirementId> {
    List<MilestoneSkillRequirementEntity> findByIdMilestoneId(Long milestoneId);
    void deleteByIdMilestoneId(Long milestoneId);
}
