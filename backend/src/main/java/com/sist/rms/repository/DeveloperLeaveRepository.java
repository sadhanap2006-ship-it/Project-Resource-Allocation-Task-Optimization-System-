package com.sist.rms.repository;

import com.sist.rms.entity.DeveloperLeave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeveloperLeaveRepository extends JpaRepository<DeveloperLeave, Long> {

    List<DeveloperLeave> findByDeveloperId(Long developerId);

    List<DeveloperLeave> findAllByOrderByStartDateAsc();
}
