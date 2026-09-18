package com.sist.rms.repository;

import com.sist.rms.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByProjectIdOrderByStartDateAsc(Long projectId);

    List<Milestone> findAllByOrderByStartDateAsc();

    /** Milestones whose end date falls inside a window - used for "upcoming deadlines". */
    List<Milestone> findByEndDateBetweenOrderByEndDateAsc(LocalDate from, LocalDate to);
}
