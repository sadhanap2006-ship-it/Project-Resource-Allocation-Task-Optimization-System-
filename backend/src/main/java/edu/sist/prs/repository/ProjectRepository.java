package edu.sist.prs.repository;

import edu.sist.prs.entity.ProjectEntity;
import edu.sist.prs.entity.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
    Optional<ProjectEntity> findByCode(String code);
    List<ProjectEntity> findByStatus(ProjectStatus status);
    List<ProjectEntity> findByPmUserId(Long pmUserId);
}
