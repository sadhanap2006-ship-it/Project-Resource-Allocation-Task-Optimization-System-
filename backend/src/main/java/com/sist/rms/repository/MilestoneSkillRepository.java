package com.sist.rms.repository;

import com.sist.rms.entity.MilestoneSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MilestoneSkillRepository extends JpaRepository<MilestoneSkill, Long> {

    List<MilestoneSkill> findByMilestoneId(Long milestoneId);

    Optional<MilestoneSkill> findByMilestoneIdAndSkillId(Long milestoneId, Long skillId);
}
