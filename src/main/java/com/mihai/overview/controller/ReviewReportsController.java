package com.mihai.overview.controller;

import com.mihai.overview.request.ReviewAverageReportRequest;
import com.mihai.overview.response.ReviewAverageReportResponse;
import com.mihai.overview.service.ReviewReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@Tag(name = "Reports", description = "Reporting endpoints (aggregations) for reviews and other metrics.")
@RequestMapping("/api/reports/reviews")
public class ReviewReportsController {

    private final ReviewReportService reviewReportService;

    @Operation(summary = "Average review score (FINAL) for selected filters", description = "Aggregates FINAL review totalScore by occurredAt range, optional agentIds, optional interactionTypeCodes.")
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/average")
    @PreAuthorize("hasAnyRole('ADMIN','QUALITY_ANALYST','TEAM_MANAGER')")
    public ReviewAverageReportResponse average(@Valid @RequestBody ReviewAverageReportRequest request) {
        return reviewReportService.getAverageScore(request);
    }
}