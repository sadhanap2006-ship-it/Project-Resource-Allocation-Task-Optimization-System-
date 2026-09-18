package com.sist.rms.repository;

import com.sist.rms.entity.DeveloperSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeveloperSkillRepository extends JpaRepository<DeveloperSkill, Long> {

    List<DeveloperSkill> findByDeveloperId(Long developerId);

    List<DeveloperSkill> findBySkillId(Long skillId);

    Optional<DeveloperSkill> findByDeveloperIdAndSkillId(Long developerId, Long skillId);

    void deleteByDeveloperIdAndSkillId(Long developerId, Long skillId);
}
