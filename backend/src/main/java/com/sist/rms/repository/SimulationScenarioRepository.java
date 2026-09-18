package com.sist.rms.repository;

import com.sist.rms.entity.SimulationScenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationScenarioRepository extends JpaRepository<SimulationScenario, Long> {

    List<SimulationScenario> findAllByOrderByCreatedAtDesc();
}
