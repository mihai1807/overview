package com.mihai.overview.service;

import com.mihai.overview.dto.request.ReviewAverageReportRequest;
import com.mihai.overview.dto.response.ReviewAverageReportResponse;

public interface ReviewReportService {
    ReviewAverageReportResponse getAverageScore(ReviewAverageReportRequest request);
}