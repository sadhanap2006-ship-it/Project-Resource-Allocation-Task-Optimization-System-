package com.sist.rms.repository;

import com.sist.rms.entity.Developer;
import com.sist.rms.entity.DeveloperStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA writes the SQL for us. We only declare the method names;
 * "findByEmail" becomes "SELECT * FROM developers WHERE email = ?".
 */
public interface DeveloperRepository extends JpaRepository<Developer, Long> {

    Optional<Developer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Developer> findByStatus(DeveloperStatus status);

    List<Developer> findAllByOrderByNameAsc();
}
