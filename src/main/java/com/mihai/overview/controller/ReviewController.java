package com.mihai.overview.controller;

import com.mihai.overview.request.CreateReviewShellRequest;
import com.mihai.overview.request.UpdateReviewCriticalHitsRequest;
import com.mihai.overview.request.UpdateReviewKpiScoresRequest;
import com.mihai.overview.request.UpdateReviewStatusRequest;
import com.mihai.overview.response.ReviewDetailsResponse;
import com.mihai.overview.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@Tag(name = "Reviews", description = "Draft reviews from schemes, fill scores, toggle final.")
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDetailsResponse> createShell(@Valid @RequestBody CreateReviewShellRequest request) {
        return ResponseEntity.ok(reviewService.createReviewShell(request));
    }

    @PatchMapping("/{reviewId}/kpi-scores")
    public ResponseEntity<ReviewDetailsResponse> updateKpiScores(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewKpiScoresRequest request
    ) {
        return ResponseEntity.ok(reviewService.updateKpiScores(reviewId, request));
    }

    @PatchMapping("/{reviewId}/critical-hits")
    public ResponseEntity<ReviewDetailsResponse> updateCriticalHits(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewCriticalHitsRequest request
    ) {
        return ResponseEntity.ok(reviewService.updateCriticalHits(reviewId, request));
    }

    @PatchMapping("/{reviewId}/status")
    public ResponseEntity<ReviewDetailsResponse> updateStatus(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewStatusRequest request
    ) {
        return ResponseEntity.ok(reviewService.updateStatus(reviewId, request));
    }
}
