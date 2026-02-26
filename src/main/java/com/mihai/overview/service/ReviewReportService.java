package com.mihai.overview.service;

import com.mihai.overview.request.ReviewAverageReportRequest;
import com.mihai.overview.response.ReviewAverageReportResponse;

public interface ReviewReportService {
    ReviewAverageReportResponse getAverageScore(ReviewAverageReportRequest request);
}