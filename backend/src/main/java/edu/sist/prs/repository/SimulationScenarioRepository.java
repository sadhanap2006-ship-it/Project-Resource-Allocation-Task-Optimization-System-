package edu.sist.prs.repository;

import edu.sist.prs.entity.SimulationScenarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SimulationScenarioRepository extends JpaRepository<SimulationScenarioEntity, Long> {
    List<SimulationScenarioEntity> findByProjectId(Long projectId);
    List<SimulationScenarioEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
