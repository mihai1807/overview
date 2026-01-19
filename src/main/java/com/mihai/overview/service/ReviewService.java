package com.mihai.overview.service;

import com.mihai.overview.entity.Review;
import com.mihai.overview.request.ReviewRequest;
import com.mihai.overview.response.ReviewResponse;
import com.mihai.overview.request.ReviewCreateWithKpisRequest;
import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(ReviewRequest reviewRequest);
    List<ReviewResponse> getAllReviewsReceivedByUserId(Long userId);
    List<ReviewResponse> getAllReviewsCreatedByUserId();
    ReviewResponse createReviewWithKpis(ReviewCreateWithKpisRequest request);
}
