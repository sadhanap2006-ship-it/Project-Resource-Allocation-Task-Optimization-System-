package edu.sist.prs.repository;

import edu.sist.prs.entity.HardwareAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HardwareAssetRepository extends JpaRepository<HardwareAssetEntity, Long> {
    Optional<HardwareAssetEntity> findByAssetTag(String assetTag);
}
