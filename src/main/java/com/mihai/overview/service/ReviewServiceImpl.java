package com.mihai.overview.service;

import com.mihai.overview.entity.*;
import com.mihai.overview.exception.BadRequestException;
import com.mihai.overview.exception.ConflictException;
import com.mihai.overview.exception.ForbiddenException;
import com.mihai.overview.exception.ResourceNotFoundException;
import com.mihai.overview.repository.CriticalConditionPoolItemRepository;
import com.mihai.overview.repository.KpiPoolItemRepository;
import com.mihai.overview.repository.ReviewRepository;
import com.mihai.overview.repository.SchemeRepository;
import com.mihai.overview.dto.request.CreateReviewShellRequest;
import com.mihai.overview.dto.request.UpdateReviewCriticalHitsRequest;
import com.mihai.overview.dto.request.UpdateReviewKpiScoresRequest;
import com.mihai.overview.dto.request.UpdateReviewStatusRequest;
import com.mihai.overview.dto.response.ReviewDetailsResponse;
import com.mihai.overview.security.FindAuthenticatedUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final SchemeRepository schemeRepository;
    private final KpiPoolItemRepository kpiPoolItemRepository;
    private final CriticalConditionPoolItemRepository criticalPoolRepository;
    private final ReviewRepository reviewRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;

    @Override
    @Transactional
    public ReviewDetailsResponse createReviewShell(CreateReviewShellRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        if (request == null) {
            throw new BadRequestException("Request body is mandatory");
        }

        Scheme scheme = schemeRepository.findById(request.getSchemeId())
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found: " + request.getSchemeId()));

        if (scheme.isArchived()) {
            throw new ConflictException("Scheme is archived");
        }

        InteractionType type = scheme.getInteractionType();

        // Allowed KPI ids from scheme rules (non-archived rules)
        List<SchemeKpiRule> schemeKpis = scheme.getKpis().stream()
                .filter(schemeKpiRule -> !schemeKpiRule.isArchived())
                .toList();

        // Allowed Critical ids from scheme rules (non-archived rules)
        List<SchemeCriticalRule> schemeCriticals = scheme.getCriticals().stream()
                .filter(schemeCriticalRule -> !schemeCriticalRule.isArchived())
                .toList();

        // Load ACTIVE pool items (archived pool items make scheme unusable for new reviews)
        Map<Long, KpiPoolItem> kpiById = kpiPoolItemRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(kpiPoolItem -> !kpiPoolItem.isArchived())
                .collect(Collectors.toMap(KpiPoolItem::getId, Function.identity()));

        Map<Long, CriticalConditionPoolItem> criticalById = criticalPoolRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(criticalConditionPoolItem -> !criticalConditionPoolItem.isArchived())
                .collect(Collectors.toMap(CriticalConditionPoolItem::getId, Function.identity()));

        // Fail fast if scheme contains missing/archived pool references
        for (SchemeKpiRule schemeKpiRule : schemeKpis) {
            if (!kpiById.containsKey(schemeKpiRule.getKpiId())) {
                throw new ConflictException("Scheme contains missing/archived KPI: " + schemeKpiRule.getKpiId());
            }
        }
        for (SchemeCriticalRule schemeCriticalRule : schemeCriticals) {
            if (!criticalById.containsKey(schemeCriticalRule.getCriticalId())) {
                throw new ConflictException("Scheme contains missing/archived Critical: " + schemeCriticalRule.getCriticalId());
            }
        }

        Review review = new Review();
        review.setScheme(scheme);
        review.setInteractionType(type);
        review.setReviewedUserId(request.getReviewedUserId());
        review.setReviewerId(currentUser.getId()); // reviewer comes from JWT
        review.setTicketId(request.getTicketId());
        review.setCid(request.getCid());
        review.setOccurredAt(request.getOccurredAt());
        review.setPeriodKey(request.getOccurredAt().format(PERIOD_FMT));

        // Draft by default, score not yet computed
        review.setStatus(ReviewStatus.DRAFT);
        review.setTotalScore(0);

        // Auto-create KPI score rows (score = null)
        for (SchemeKpiRule schemeKpiRule : schemeKpis) {
            KpiPoolItem kpi = kpiById.get(schemeKpiRule.getKpiId());
            ReviewKpiScore line = new ReviewKpiScore();
            line.setReview(review);
            line.setKpi(kpi);
            line.setScore(null);
            review.getKpiScores().add(line);
        }

        // Auto-create critical hit rows (triggered = false)
        for (SchemeCriticalRule schemeCriticalRule : schemeCriticals) {
            CriticalConditionPoolItem criticalConditionPoolItem = criticalById.get(schemeCriticalRule.getCriticalId());
            ReviewCriticalHit hit = new ReviewCriticalHit();
            hit.setReview(review);
            hit.setCritical(criticalConditionPoolItem);
            hit.setTriggered(false);
            review.getCriticalHits().add(hit);
        }

        Review saved = reviewRepository.save(review);
        return toDetailsResponse(saved, kpiById, criticalById, schemeKpis, schemeCriticals);
    }

    @Override
    @Transactional
    public ReviewDetailsResponse updateKpiScores(Long reviewId, UpdateReviewKpiScoresRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        if (request == null) {
            throw new BadRequestException("Request body is mandatory");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        enforceOwner(review, currentUser);

        Scheme scheme = review.getScheme();
        if (scheme.isArchived()) {
            throw new ConflictException("Scheme is archived");
        }

        InteractionType type = review.getInteractionType();

        List<SchemeKpiRule> schemeKpis = scheme.getKpis().stream()
                .filter(schemeKpiRule -> !schemeKpiRule.isArchived())
                .toList();

        Set<Long> expectedKpiIds = schemeKpis.stream()
                .map(SchemeKpiRule::getKpiId)
                .collect(Collectors.toSet());

        // Validate request: must include ALL scheme KPIs, no duplicates
        Set<Long> seen = new HashSet<>();
        for (UpdateReviewKpiScoresRequest.KpiScoreInput kpiScoreInput : request.getKpiScores()) {
            if (!seen.add(kpiScoreInput.getKpiId())) {
                throw new BadRequestException("Duplicate KPI id in request: " + kpiScoreInput.getKpiId());
            }
            if (!expectedKpiIds.contains(kpiScoreInput.getKpiId())) {
                throw new BadRequestException("KPI not part of scheme: " + kpiScoreInput.getKpiId());
            }
        }
        if (seen.size() != expectedKpiIds.size()) {
            throw new BadRequestException("All scheme KPIs must be scored. Expected: " +
                    expectedKpiIds.size() + ", provided: " + seen.size());
        }

        // ACTIVE pool KPIs map (weights)
        Map<Long, KpiPoolItem> kpiById = kpiPoolItemRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(kpiPoolItem -> !kpiPoolItem.isArchived())
                .collect(Collectors.toMap(KpiPoolItem::getId, Function.identity()));

        // Validate scheme references still exist in ACTIVE pool
        for (Long id : expectedKpiIds) {
            if (!kpiById.containsKey(id)) {
                throw new ConflictException("Scheme contains missing/archived KPI: " + id);
            }
        }

        // Map existing review KPI rows by KPI id
        Map<Long, ReviewKpiScore> existingLines = review.getKpiScores().stream()
                .collect(Collectors.toMap(l -> l.getKpi().getId(), Function.identity()));

        // Apply updates
        for (UpdateReviewKpiScoresRequest.KpiScoreInput kpiScoreInput : request.getKpiScores()) {
            ReviewKpiScore line = existingLines.get(kpiScoreInput.getKpiId());
            if (line == null) {
                // Indicates inconsistent DB state; still returned as 400 via your global handler
                throw new ConflictException("Review is missing KPI row for: " + kpiScoreInput.getKpiId());
            }
            line.setScore(kpiScoreInput.getScore());
        }

        // Recompute totalScore: sum(score * weight) / 100, with weights coming from pool
        int weightedSum = 0;
        int weightSum = 0;

        for (UpdateReviewKpiScoresRequest.KpiScoreInput kpiScoreInput : request.getKpiScores()) {
            KpiPoolItem kpi = kpiById.get(kpiScoreInput.getKpiId());
            int w = kpi.getWeightPercent();
            weightedSum += kpiScoreInput.getScore() * w;
            weightSum += w;
        }

        if (weightSum != 100) {
            throw new BadRequestException("Scheme KPI weights must sum to 100. Current: " + weightSum);
        }

        int totalScore = Math.round(weightedSum / 100.0f);
        review.setTotalScore(totalScore);

        Review saved = reviewRepository.save(review);

        // For response enrichment (critical)
        List<SchemeCriticalRule> schemeCriticals = scheme.getCriticals().stream()
                .filter(schemeCriticalRule -> !schemeCriticalRule.isArchived())
                .toList();

        Map<Long, CriticalConditionPoolItem> criticalById = criticalPoolRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(criticalConditionPoolItem -> !criticalConditionPoolItem.isArchived())
                .collect(Collectors.toMap(CriticalConditionPoolItem::getId, Function.identity()));

        return toDetailsResponse(saved, kpiById, criticalById, schemeKpis, schemeCriticals);
    }

    @Override
    @Transactional
    public ReviewDetailsResponse updateCriticalHits(Long reviewId, UpdateReviewCriticalHitsRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        if (request == null) {
            throw new BadRequestException("Request body is mandatory");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        enforceOwner(review, currentUser);

        Scheme scheme = review.getScheme();
        if (scheme.isArchived()) {
            throw new ConflictException("Scheme is archived");
        }

        InteractionType type = review.getInteractionType();

        List<SchemeCriticalRule> schemeCriticals = scheme.getCriticals().stream()
                .filter(schemeCriticalRule -> !schemeCriticalRule.isArchived())
                .toList();

        Set<Long> allowedCriticalIds = schemeCriticals.stream()
                .map(SchemeCriticalRule::getCriticalId)
                .collect(Collectors.toSet());

        List<Long> triggered = request.getTriggeredCriticalIds() == null
                ? Collections.emptyList()
                : request.getTriggeredCriticalIds();

        // Validate triggered IDs are subset of scheme criticals, no duplicates
        Set<Long> seen = new HashSet<>();
        for (Long id : triggered) {
            if (!seen.add(id)) {
                throw new BadRequestException("Duplicate triggered critical id: " + id);
            }
            if (!allowedCriticalIds.contains(id)) {
                throw new BadRequestException("Critical not part of scheme: " + id);
            }
        }

        // Map existing review critical rows by critical id
        Map<Long, ReviewCriticalHit> hitByCriticalId = review.getCriticalHits().stream()
                .collect(Collectors.toMap(reviewCriticalHit -> reviewCriticalHit.getCritical().getId(), Function.identity()));

        // First set all to false
        for (Long id : allowedCriticalIds) {
            ReviewCriticalHit hit = hitByCriticalId.get(id);
            if (hit == null) {
                throw new ConflictException("Review is missing Critical row for: " + id);
            }
            hit.setTriggered(false);
        }

        // Then set triggered ones to true
        for (Long id : triggered) {
            ReviewCriticalHit hit = hitByCriticalId.get(id);
            hit.setTriggered(true);
        }

        Review saved = reviewRepository.save(review);

        // For response enrichment (active pools)
        List<SchemeKpiRule> schemeKpis = scheme.getKpis().stream()
                .filter(schemeKpiRule -> !schemeKpiRule.isArchived())
                .toList();

        Map<Long, KpiPoolItem> kpiById = kpiPoolItemRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(kpiPoolItem -> !kpiPoolItem.isArchived())
                .collect(Collectors.toMap(KpiPoolItem::getId, Function.identity()));

        Map<Long, CriticalConditionPoolItem> criticalById = criticalPoolRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(criticalConditionPoolItem -> !criticalConditionPoolItem.isArchived())
                .collect(Collectors.toMap(CriticalConditionPoolItem::getId, Function.identity()));

        // Optional extra safety: if scheme contains missing/archived pool criticals, fail here too
        for (SchemeCriticalRule schemeCriticalRule : schemeCriticals) {
            if (!criticalById.containsKey(schemeCriticalRule.getCriticalId())) {
                throw new ConflictException("Scheme contains missing/archived Critical: " + schemeCriticalRule.getCriticalId());
            }
        }

        return toDetailsResponse(saved, kpiById, criticalById, schemeKpis, schemeCriticals);
    }

    @Override
    @Transactional
    public ReviewDetailsResponse updateStatus(Long reviewId, UpdateReviewStatusRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        if (request == null) {
            throw new BadRequestException("Request body is mandatory");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        enforceOwner(review, currentUser);

        ReviewStatus newStatus = request.getStatus();

        if (newStatus == ReviewStatus.FINAL) {
            // Must have all KPI scores filled
            for (ReviewKpiScore line : review.getKpiScores()) {
                if (line.getScore() == null) {
                    throw new ConflictException("Cannot finalize review: some KPI scores are missing");
                }
            }
            review.setStatus(ReviewStatus.FINAL);
            review.setFinalizedAt(Instant.now());
            review.setFinalizedByUserId(currentUser.getId());
        } else {
            // Back to draft
            review.setStatus(ReviewStatus.DRAFT);
            review.setFinalizedAt(null);
            review.setFinalizedByUserId(null);
        }

        Review saved = reviewRepository.save(review);

        // For response enrichment
        Scheme scheme = saved.getScheme();
        InteractionType type = saved.getInteractionType();

        List<SchemeKpiRule> schemeKpis = scheme.getKpis().stream()
                .filter(schemeKpiRule -> !schemeKpiRule.isArchived())
                .toList();

        List<SchemeCriticalRule> schemeCriticals = scheme.getCriticals().stream()
                .filter(schemeCriticalRule -> !schemeCriticalRule.isArchived())
                .toList();

        Map<Long, KpiPoolItem> kpiById = kpiPoolItemRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(kpiPoolItem -> !kpiPoolItem.isArchived())
                .collect(Collectors.toMap(KpiPoolItem::getId, Function.identity()));

        Map<Long, CriticalConditionPoolItem> criticalById = criticalPoolRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(criticalConditionPoolItem -> !criticalConditionPoolItem.isArchived())
                .collect(Collectors.toMap(CriticalConditionPoolItem::getId, Function.identity()));

        // Fail fast if scheme contains missing/archived pool references (consistent error)
        for (SchemeKpiRule schemeKpiRule : schemeKpis) {
            if (!kpiById.containsKey(schemeKpiRule.getKpiId())) {
                throw new ConflictException("Scheme contains missing/archived KPI: " + schemeKpiRule.getKpiId());
            }
        }
        for (SchemeCriticalRule schemeCriticalRule : schemeCriticals) {
            if (!criticalById.containsKey(schemeCriticalRule.getCriticalId())) {
                throw new ConflictException("Scheme contains missing/archived Critical: " + schemeCriticalRule.getCriticalId());
            }
        }

        return toDetailsResponse(saved, kpiById, criticalById, schemeKpis, schemeCriticals);
    }

    private void enforceOwner(Review review, User currentUser) {
        if (!Objects.equals(review.getReviewerId(), currentUser.getId())) {
            throw new ForbiddenException("You can only edit reviews you created");
        }
    }

    private ReviewDetailsResponse toDetailsResponse(
            Review review,
            Map<Long, KpiPoolItem> kpiById,
            Map<Long, CriticalConditionPoolItem> criticalById,
            List<SchemeKpiRule> schemeKpis,
            List<SchemeCriticalRule> schemeCriticals
    ) {
        // KPI lines in scheme order
        List<ReviewDetailsResponse.KpiLine> kpis = schemeKpis.stream()
                .sorted(Comparator.comparingInt(SchemeKpiRule::getOrderIndex))
                .map(schemeKpiRule -> {
                    KpiPoolItem kpi = kpiById.get(schemeKpiRule.getKpiId());
                    if (kpi == null) {
                        throw new ConflictException("Scheme contains missing/archived KPI: " + schemeKpiRule.getKpiId());
                    }

                    // Find review line for score (it may be null)
                    Integer score = review.getKpiScores().stream()
                            .filter(reviewKpiScore -> reviewKpiScore.getKpi().getId().equals(kpi.getId()))
                            .findFirst()
                            .map(ReviewKpiScore::getScore)
                            .orElse(null);

                    return new ReviewDetailsResponse.KpiLine(
                            kpi.getId(),
                            kpi.getName(),
                            kpi.getWeightPercent(),
                            schemeKpiRule.isRequired(),
                            score
                    );
                })
                .toList();

        // Critical lines in scheme order
        List<ReviewDetailsResponse.CriticalLine> criticals = schemeCriticals.stream()
                .sorted(Comparator.comparingInt(SchemeCriticalRule::getOrderIndex))
                .map(schemeCriticalRule -> {
                    CriticalConditionPoolItem criticalConditionPoolItem = criticalById.get(schemeCriticalRule.getCriticalId());
                    if (criticalConditionPoolItem == null) {
                        throw new ConflictException("Scheme contains missing/archived Critical: " + schemeCriticalRule.getCriticalId());
                    }

                    boolean triggered = review.getCriticalHits().stream()
                            .filter(reviewCriticalHit -> reviewCriticalHit.getCritical().getId().equals(criticalConditionPoolItem.getId()))
                            .map(ReviewCriticalHit::isTriggered)
                            .findFirst()
                            .orElse(false);

                    return new ReviewDetailsResponse.CriticalLine(
                            criticalConditionPoolItem.getId(),
                            criticalConditionPoolItem.getName(),
                            triggered
                    );
                })
                .toList();

        // totalScore: expose null if still draft with missing KPI scores
        boolean hasMissing = review.getKpiScores().stream().anyMatch(reviewKpiScore -> reviewKpiScore.getScore() == null);
        Integer exposedTotal = hasMissing ? null : review.getTotalScore();

        return new ReviewDetailsResponse(
                review.getId(),
                review.getScheme().getId(),
                review.getInteractionType().getCode(),
                review.getPeriodKey(),
                exposedTotal,
                review.getStatus(),
                kpis,
                criticals
        );
    }
}
