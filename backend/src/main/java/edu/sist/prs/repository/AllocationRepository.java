package edu.sist.prs.repository;

import edu.sist.prs.entity.AllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationRepository extends JpaRepository<AllocationEntity, Long> {

    List<AllocationEntity> findByMilestoneId(Long milestoneId);

    List<AllocationEntity> findByResourceId(Long resourceId);

    Optional<AllocationEntity> findByResourceIdAndMilestoneIdAndStartDate(Long resourceId, Long milestoneId, LocalDate startDate);

    @Query("SELECT a FROM AllocationEntity a WHERE a.resourceId = :resourceId AND a.startDate <= :endDate AND a.endDate >= :startDate")
    List<AllocationEntity> findOverlappingByResource(
        @Param("resourceId") Long resourceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT a FROM AllocationEntity a JOIN a.milestone m WHERE m.projectId = :projectId")
    List<AllocationEntity> findByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT a FROM AllocationEntity a JOIN a.milestone m WHERE m.projectId = :projectId AND a.startDate <= :endDate AND a.endDate >= :startDate")
    List<AllocationEntity> findOverlappingByProject(
        @Param("projectId") Long projectId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
