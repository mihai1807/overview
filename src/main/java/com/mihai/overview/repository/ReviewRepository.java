package com.mihai.overview.repository;

import com.mihai.overview.entity.Review;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends CrudRepository<Review, Long> {
    List<Review> findByReviewerId (Long reviewerId);
    List<Review> findByReviewedUserId (Long reviewedUserId);
    boolean existsByKpiScheme_Id(Long schemeId);
}
