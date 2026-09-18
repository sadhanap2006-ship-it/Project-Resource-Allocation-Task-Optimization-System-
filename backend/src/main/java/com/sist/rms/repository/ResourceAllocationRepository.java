package com.sist.rms.repository;

import com.sist.rms.entity.ResourceAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceAllocationRepository extends JpaRepository<ResourceAllocation, Long> {

    List<ResourceAllocation> findByDeveloperId(Long developerId);

    List<ResourceAllocation> findByProjectId(Long projectId);

    List<ResourceAllocation> findByMilestoneId(Long milestoneId);

    List<ResourceAllocation> findAllByOrderByStartDateAsc();
}
