package com.mihai.overview.repository;

import com.mihai.overview.entity.ReviewKpiScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewKpiScoreRepository extends JpaRepository<ReviewKpiScore, Long> {
    List<ReviewKpiScore> findByReview_Id(Long reviewId);
}
