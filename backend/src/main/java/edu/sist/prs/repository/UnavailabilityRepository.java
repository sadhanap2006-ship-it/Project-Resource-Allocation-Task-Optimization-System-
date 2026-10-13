package edu.sist.prs.repository;

import edu.sist.prs.entity.UnavailabilityEntity;
import edu.sist.prs.entity.UnavailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface UnavailabilityRepository extends JpaRepository<UnavailabilityEntity, Long> {

    List<UnavailabilityEntity> findByResourceId(Long resourceId);

    List<UnavailabilityEntity> findByStatus(UnavailabilityStatus status);

    @Query("SELECT u FROM UnavailabilityEntity u WHERE u.resourceId = :resourceId AND u.status = :status AND u.startDate <= :endDate AND u.endDate >= :startDate")
    List<UnavailabilityEntity> findOverlappingByResourceAndStatus(
        @Param("resourceId") Long resourceId,
        @Param("status") UnavailabilityStatus status,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT u FROM UnavailabilityEntity u WHERE u.resourceId = :resourceId AND u.startDate <= :endDate AND u.endDate >= :startDate")
    List<UnavailabilityEntity> findOverlappingByResource(
        @Param("resourceId") Long resourceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
