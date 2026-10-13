package edu.sist.prs.repository;

import edu.sist.prs.entity.ResourceSkillEntity;
import edu.sist.prs.entity.ResourceSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResourceSkillRepository extends JpaRepository<ResourceSkillEntity, ResourceSkillId> {
    List<ResourceSkillEntity> findByIdResourceId(Long resourceId);
    List<ResourceSkillEntity> findByIdSkillId(Long skillId);
    void deleteByIdResourceId(Long resourceId);
}
