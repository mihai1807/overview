package com.mihai.overview.repository;

import com.mihai.overview.entity.ReviewCriticalHit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCriticalHitRepository extends JpaRepository<ReviewCriticalHit, Long> {
    boolean existsByCritical_Id(Long criticalId);
}
