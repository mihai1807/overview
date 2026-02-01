package com.mihai.overview.service;

import com.mihai.overview.request.CreateReviewShellRequest;
import com.mihai.overview.request.UpdateReviewCriticalHitsRequest;
import com.mihai.overview.request.UpdateReviewKpiScoresRequest;
import com.mihai.overview.request.UpdateReviewStatusRequest;
import com.mihai.overview.response.ReviewDetailsResponse;

public interface ReviewService {
    ReviewDetailsResponse createReviewShell(CreateReviewShellRequest request);
    ReviewDetailsResponse updateKpiScores(Long reviewId, UpdateReviewKpiScoresRequest request);
    ReviewDetailsResponse updateCriticalHits(Long reviewId, UpdateReviewCriticalHitsRequest request);
    ReviewDetailsResponse updateStatus(Long reviewId, UpdateReviewStatusRequest request);
}
