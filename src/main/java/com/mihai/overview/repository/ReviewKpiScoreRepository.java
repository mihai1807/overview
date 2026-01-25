package com.mihai.overview.repository;

import com.mihai.overview.entity.ReviewKpiScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewKpiScoreRepository extends JpaRepository<ReviewKpiScore, Long> {
    boolean existsByKpi_Id(Long kpiId);
}
