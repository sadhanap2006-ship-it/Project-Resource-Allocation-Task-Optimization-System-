package com.sist.rms.repository;

import com.sist.rms.entity.ProjectBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectBudgetRepository extends JpaRepository<ProjectBudget, Long> {

    Optional<ProjectBudget> findByProjectId(Long projectId);
}
