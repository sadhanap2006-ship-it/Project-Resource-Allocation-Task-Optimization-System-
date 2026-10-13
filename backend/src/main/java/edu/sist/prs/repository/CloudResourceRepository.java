package edu.sist.prs.repository;

import edu.sist.prs.entity.CloudResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CloudResourceRepository extends JpaRepository<CloudResourceEntity, Long> {
}
