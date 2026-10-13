package edu.sist.prs.repository;

import edu.sist.prs.entity.SimulationChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SimulationChangeRepository extends JpaRepository<SimulationChangeEntity, Long> {
    List<SimulationChangeEntity> findByScenarioId(Long scenarioId);
}
