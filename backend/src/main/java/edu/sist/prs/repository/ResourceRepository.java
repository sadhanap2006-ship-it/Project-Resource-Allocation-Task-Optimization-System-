package edu.sist.prs.repository;

import edu.sist.prs.entity.ResourceEntity;
import edu.sist.prs.entity.ResourceStatus;
import edu.sist.prs.entity.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<ResourceEntity, Long> {
    List<ResourceEntity> findByTypeAndStatus(ResourceType type, ResourceStatus status);
    List<ResourceEntity> findByStatus(ResourceStatus status);
    List<ResourceEntity> findByType(ResourceType type);
}
