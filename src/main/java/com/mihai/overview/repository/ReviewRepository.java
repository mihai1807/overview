package com.mihai.overview.repository;

import com.mihai.overview.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByReviewedUserIdAndPeriodKey(Long reviewedUserId, String periodKey);
    boolean existsByScheme_Id(Long schemeId);
}