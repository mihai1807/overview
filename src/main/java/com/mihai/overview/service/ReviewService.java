package com.mihai.overview.service;

import com.mihai.overview.request.CreateReviewRequest;
import com.mihai.overview.response.ReviewResponse;

public interface ReviewService {
    ReviewResponse createReview(CreateReviewRequest request);
}
