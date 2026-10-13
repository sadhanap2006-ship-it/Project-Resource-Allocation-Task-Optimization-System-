package edu.sist.prs.repository;

import edu.sist.prs.entity.DeveloperProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DeveloperProfileRepository extends JpaRepository<DeveloperProfileEntity, Long> {
    Optional<DeveloperProfileEntity> findByEmail(String email);
}
