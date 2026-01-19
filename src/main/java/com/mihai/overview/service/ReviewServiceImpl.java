package com.mihai.overview.service;

import com.mihai.overview.entity.KpiSchemeItem;
import com.mihai.overview.entity.Review;
import com.mihai.overview.entity.User;
import com.mihai.overview.repository.KpiSchemeItemRepository;
import com.mihai.overview.repository.ReviewKpiScoreRepository;
import com.mihai.overview.repository.ReviewRepository;
import com.mihai.overview.repository.ReviewTypeRepository;
import com.mihai.overview.request.ReviewCreateWithKpisRequest;
import com.mihai.overview.request.ReviewRequest;
import com.mihai.overview.response.ReviewResponse;
import com.mihai.overview.util.FindAuthenticatedUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mihai.overview.entity.ReviewType;
import com.mihai.overview.entity.KpiScheme;

import java.util.List;

@Service
@AllArgsConstructor
public class ReviewServiceImpl implements ReviewService{

    private final ReviewRepository reviewRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final ReviewTypeRepository reviewTypeRepository;
    private final KpiSchemeItemRepository kpiSchemeItemRepository;
    private final ReviewKpiScoreRepository reviewKpiScoreRepository;

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

    @Override
    @Transactional
    public ReviewResponse createReviewWithKpis(ReviewCreateWithKpisRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        ReviewType type = reviewTypeRepository.findByCode(request.getReviewTypeCode())
                .orElseThrow(() -> new IllegalArgumentException("ReviewType not found: " + request.getReviewTypeCode()));

        KpiScheme activeScheme = type.getActiveScheme();
        if (activeScheme == null) {
            throw new IllegalStateException("No active KPI scheme configured for ReviewType: " + type.getCode());
        }

        // load scheme items for validation and weight calculation
        List<KpiSchemeItem> schemeItems = kpiSchemeItemRepository.findByScheme_Id(activeScheme.getId());

        int totalWeight = schemeItems.stream().mapToInt(KpiSchemeItem::getWeightPercent).sum();
        if (totalWeight != 100) {
            throw new IllegalStateException("Scheme weights must total 100%. Current total: " + totalWeight);
        }

        // Build lookup by itemId
        java.util.Map<Long, KpiSchemeItem> itemById = schemeItems.stream()
                .collect(java.util.stream.Collectors.toMap(KpiSchemeItem::getId, x -> x));

        // Validate required items present + validate score ranges
        java.util.Set<Long> providedItemIds = request.getKpis().stream()
                .map(com.mihai.overview.request.ReviewKpiScoreRequest::getSchemeItemId)
                .collect(java.util.stream.Collectors.toSet());

        for (KpiSchemeItem item : schemeItems) {
            if (item.isRequired() && !providedItemIds.contains(item.getId())) {
                throw new IllegalArgumentException("Missing required KPI: " + item.getKpi().getName());
            }
        }

        double total = 0.0;

        for (com.mihai.overview.request.ReviewKpiScoreRequest input : request.getKpis()) {
            KpiSchemeItem item = itemById.get(input.getSchemeItemId());
            if (item == null) {
                throw new IllegalArgumentException("Scheme item not part of active scheme: " + input.getSchemeItemId());
            }

            int score = input.getScore();
            if (score < item.getMinScore() || score > item.getMaxScore()) {
                throw new IllegalArgumentException(
                        "Score out of range for KPI " + item.getKpi().getName()
                                + ". Allowed: " + item.getMinScore() + " to " + item.getMaxScore()
                                + ", got: " + score
                );
            }

            // contribution = (score/max) * weightPercent
            total += ((double) score / (double) item.getMaxScore()) * (double) item.getWeightPercent();
        }

        int finalScore = (int) Math.round(total);

        Review review = new Review(
                type.getCode(),                 // keep your existing string field
                request.getReviewedUserId(),
                request.getTicketId(),
                request.getInteractionDate(),
                request.getInteractionTime(),
                request.getCid(),
                finalScore,                     // computed
                false,
                currentUser.getId()
        );
        review.setKpiScheme(activeScheme);

        Review savedReview = reviewRepository.save(review);

        // Persist per-item scores
        for (com.mihai.overview.request.ReviewKpiScoreRequest input : request.getKpis()) {
            KpiSchemeItem item = itemById.get(input.getSchemeItemId());

            com.mihai.overview.entity.ReviewKpiScore scoreRow = new com.mihai.overview.entity.ReviewKpiScore();
            scoreRow.setReview(savedReview);
            scoreRow.setSchemeItem(item);
            scoreRow.setScore(input.getScore());

            reviewKpiScoreRepository.save(scoreRow);
        }

        return new ReviewResponse(
                savedReview.getId(),
                savedReview.getTicketId(),
                savedReview.getReviewedUserId(),
                savedReview.getInteractionScore()
        );
    }

}
