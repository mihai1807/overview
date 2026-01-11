package com.mihai.overview.service;

import com.mihai.overview.entity.Review;
import com.mihai.overview.entity.User;
import com.mihai.overview.repository.ReviewRepository;
import com.mihai.overview.request.ReviewRequest;
import com.mihai.overview.response.ReviewResponse;
import com.mihai.overview.util.FindAuthenticatedUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class ReviewServiceImpl implements ReviewService{

    private final ReviewRepository reviewRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;

    @Override
    @Transactional
    public ReviewResponse createReview(ReviewRequest reviewRequest) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        Review review = new Review(
                reviewRequest.getReviewType(),
                reviewRequest.getReviewedUserId(),
                reviewRequest.getTicketId(),
                reviewRequest.getInteractionDate(),
                reviewRequest.getInteractionTime(),
                reviewRequest.getCid(),
                reviewRequest.getInteractionScore(),
                false,
                currentUser.getId()
        );

        Review savedReview = reviewRepository.save(review);

        return new ReviewResponse(
                savedReview.getId(),
                savedReview.getTicketId(),
                savedReview.getReviewedUserId(),
                savedReview.getInteractionScore()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviewsReceivedByUserId(Long userId) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();
        if (currentUser == null) {
            throw new SecurityException("Unauthorized access: user not authenticated");
        }
        return reviewRepository.findByReviewedUserId(userId)
                .stream()
                .map(this::convertToReviewResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviewsCreatedByUserId() {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();
        if (currentUser == null) {
            throw new SecurityException("Unauthorized access: user not authenticated");
        }
        return reviewRepository.findByReviewerId(currentUser.getId())
                .stream()
                .map(this::convertToReviewResponse)
                .toList();
    }

    private ReviewResponse convertToReviewResponse (Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getTicketId(),
                review.getReviewedUserId(),
                review.getInteractionScore()
        );
    }
}
