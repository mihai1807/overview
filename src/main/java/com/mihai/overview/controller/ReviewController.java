package com.mihai.overview.controller;

import com.mihai.overview.request.ReviewRequest;
import com.mihai.overview.response.ReviewResponse;
import com.mihai.overview.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "Reviews REST API", description = "Operations for managing reviews")
@AllArgsConstructor
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Create a review", description = "Create a review for another user")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ReviewResponse createReview(@Valid @RequestBody ReviewRequest reviewRequest) {
        return reviewService.createReview(reviewRequest);
    }

    @Operation(summary = "View received reviews", description = "View all reviews created for a specific user by ID")
    @GetMapping("/received/{userId}")
    public ResponseEntity<List<ReviewResponse>> getReceivedReviews(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getAllReviewsReceivedByUserId(userId));
    }

    @Operation(summary = "View created reviews", description = "View all reviews created by the logged in user")
    @GetMapping("/created")
    public ResponseEntity<List<ReviewResponse>> getCreatedReviews() {
        return ResponseEntity.ok(reviewService.getAllReviewsCreatedByUserId());
    }

}
