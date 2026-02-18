package com.mihai.overview.service;

import com.mihai.overview.entity.*;
import com.mihai.overview.repository.CriticalConditionPoolItemRepository;
import com.mihai.overview.repository.KpiPoolItemRepository;
import com.mihai.overview.repository.ReviewRepository;
import com.mihai.overview.repository.SchemeRepository;
import com.mihai.overview.request.CreateReviewShellRequest;
import com.mihai.overview.request.UpdateReviewCriticalHitsRequest;
import com.mihai.overview.request.UpdateReviewKpiScoresRequest;
import com.mihai.overview.request.UpdateReviewStatusRequest;
import com.mihai.overview.response.ReviewDetailsResponse;
import com.mihai.overview.util.FindAuthenticatedUser;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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

        Scheme scheme = schemeRepository.findById(request.getSchemeId())
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + request.getSchemeId()));

        if (scheme.isArchived()) {
            throw new IllegalArgumentException("Scheme is archived");
        }

        InteractionType type = scheme.getInteractionType();

        // Allowed KPI ids from scheme rules (non-archived rules)
        List<SchemeKpiRule> schemeKpis = scheme.getKpis().stream()
                .filter(r -> !r.isArchived())
                .toList();

        // Allowed Critical ids from scheme rules (non-archived rules)
        List<SchemeCriticalRule> schemeCriticals = scheme.getCriticals().stream()
                .filter(r -> !r.isArchived())
                .toList();

        // Load ACTIVE pool items (archived pool items make scheme unusable for new reviews)
        Map<Long, KpiPoolItem> kpiById = kpiPoolItemRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(k -> !k.isArchived())
                .collect(Collectors.toMap(KpiPoolItem::getId, Function.identity()));

        Map<Long, CriticalConditionPoolItem> criticalById = criticalPoolRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(c -> !c.isArchived())
                .collect(Collectors.toMap(CriticalConditionPoolItem::getId, Function.identity()));

        // Fail fast if scheme contains missing/archived pool references
        for (SchemeKpiRule r : schemeKpis) {
            if (!kpiById.containsKey(r.getKpiId())) {
                throw new IllegalArgumentException("Scheme contains missing/archived KPI: " + r.getKpiId());
            }
        }
        for (SchemeCriticalRule r : schemeCriticals) {
            if (!criticalById.containsKey(r.getCriticalId())) {
                throw new IllegalArgumentException("Scheme contains missing/archived Critical: " + r.getCriticalId());
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
        for (SchemeKpiRule r : schemeKpis) {
            KpiPoolItem kpi = kpiById.get(r.getKpiId());
            ReviewKpiScore line = new ReviewKpiScore();
            line.setReview(review);
            line.setKpi(kpi);
            line.setScore(null);
            review.getKpiScores().add(line);
        }

        // Auto-create critical hit rows (triggered = false)
        for (SchemeCriticalRule r : schemeCriticals) {
            CriticalConditionPoolItem c = criticalById.get(r.getCriticalId());
            ReviewCriticalHit hit = new ReviewCriticalHit();
            hit.setReview(review);
            hit.setCritical(c);
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

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));

        enforceOwner(review, currentUser);

        Scheme scheme = review.getScheme();
        if (scheme.isArchived()) {
            throw new IllegalArgumentException("Scheme is archived");
        }

        InteractionType type = review.getInteractionType();

        List<SchemeKpiRule> schemeKpis = scheme.getKpis().stream()
                .filter(r -> !r.isArchived())
                .toList();

        Set<Long> expectedKpiIds = schemeKpis.stream()
                .map(SchemeKpiRule::getKpiId)
                .collect(Collectors.toSet());

        // Validate request: must include ALL scheme KPIs, no duplicates
        Set<Long> seen = new HashSet<>();
        for (UpdateReviewKpiScoresRequest.KpiScoreInput in : request.getKpiScores()) {
            if (!seen.add(in.getKpiId())) {
                throw new IllegalArgumentException("Duplicate KPI id in request: " + in.getKpiId());
            }
            if (!expectedKpiIds.contains(in.getKpiId())) {
                throw new IllegalArgumentException("KPI not part of scheme: " + in.getKpiId());
            }
        }
        if (seen.size() != expectedKpiIds.size()) {
            throw new IllegalArgumentException("All scheme KPIs must be scored. Expected: " +
                    expectedKpiIds.size() + ", provided: " + seen.size());
        }

        // ACTIVE pool KPIs map (weights)
        Map<Long, KpiPoolItem> kpiById = kpiPoolItemRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(k -> !k.isArchived())
                .collect(Collectors.toMap(KpiPoolItem::getId, Function.identity()));

        // Validate scheme references still exist in ACTIVE pool
        for (Long id : expectedKpiIds) {
            if (!kpiById.containsKey(id)) {
                throw new IllegalArgumentException("Scheme contains missing/archived KPI: " + id);
            }
        }

        // Map existing review KPI rows by KPI id
        Map<Long, ReviewKpiScore> existingLines = review.getKpiScores().stream()
                .collect(Collectors.toMap(l -> l.getKpi().getId(), Function.identity()));

        // Apply updates
        for (UpdateReviewKpiScoresRequest.KpiScoreInput in : request.getKpiScores()) {
            ReviewKpiScore line = existingLines.get(in.getKpiId());
            if (line == null) {
                // Indicates inconsistent DB state; still returned as 400 via your global handler
                throw new IllegalArgumentException("Review is missing KPI row for: " + in.getKpiId());
            }
            line.setScore(in.getScore());
        }

        // Recompute totalScore: sum(score * weight) / 100, with weights coming from pool
        int weightedSum = 0;
        int weightSum = 0;

        for (UpdateReviewKpiScoresRequest.KpiScoreInput in : request.getKpiScores()) {
            KpiPoolItem kpi = kpiById.get(in.getKpiId());
            int w = kpi.getWeightPercent();
            weightedSum += in.getScore() * w;
            weightSum += w;
        }

        if (weightSum != 100) {
            throw new IllegalArgumentException("Scheme KPI weights must sum to 100. Current: " + weightSum);
        }

        int totalScore = Math.round(weightedSum / 100.0f);
        review.setTotalScore(totalScore);

        Review saved = reviewRepository.save(review);

        // For response enrichment (critical)
        List<SchemeCriticalRule> schemeCriticals = scheme.getCriticals().stream()
                .filter(r -> !r.isArchived())
                .toList();

        Map<Long, CriticalConditionPoolItem> criticalById = criticalPoolRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(c -> !c.isArchived())
                .collect(Collectors.toMap(CriticalConditionPoolItem::getId, Function.identity()));

        return toDetailsResponse(saved, kpiById, criticalById, schemeKpis, schemeCriticals);
    }

    @Override
    @Transactional
    public ReviewDetailsResponse updateCriticalHits(Long reviewId, UpdateReviewCriticalHitsRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));

        enforceOwner(review, currentUser);

        Scheme scheme = review.getScheme();
        if (scheme.isArchived()) {
            throw new IllegalArgumentException("Scheme is archived");
        }

        InteractionType type = review.getInteractionType();

        List<SchemeCriticalRule> schemeCriticals = scheme.getCriticals().stream()
                .filter(r -> !r.isArchived())
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
                throw new IllegalArgumentException("Duplicate triggered critical id: " + id);
            }
            if (!allowedCriticalIds.contains(id)) {
                throw new IllegalArgumentException("Critical not part of scheme: " + id);
            }
        }

        // Map existing review critical rows by critical id
        Map<Long, ReviewCriticalHit> hitByCriticalId = review.getCriticalHits().stream()
                .collect(Collectors.toMap(h -> h.getCritical().getId(), Function.identity()));

        // First set all to false
        for (Long id : allowedCriticalIds) {
            ReviewCriticalHit hit = hitByCriticalId.get(id);
            if (hit == null) {
                throw new IllegalArgumentException("Review is missing Critical row for: " + id);
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
                .filter(r -> !r.isArchived())
                .toList();

        Map<Long, KpiPoolItem> kpiById = kpiPoolItemRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(k -> !k.isArchived())
                .collect(Collectors.toMap(KpiPoolItem::getId, Function.identity()));

        Map<Long, CriticalConditionPoolItem> criticalById = criticalPoolRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(c -> !c.isArchived())
                .collect(Collectors.toMap(CriticalConditionPoolItem::getId, Function.identity()));

        // Optional extra safety: if scheme contains missing/archived pool criticals, fail here too
        for (SchemeCriticalRule r : schemeCriticals) {
            if (!criticalById.containsKey(r.getCriticalId())) {
                throw new IllegalArgumentException("Scheme contains missing/archived Critical: " + r.getCriticalId());
            }
        }

        return toDetailsResponse(saved, kpiById, criticalById, schemeKpis, schemeCriticals);
    }

    @Override
    @Transactional
    public ReviewDetailsResponse updateStatus(Long reviewId, UpdateReviewStatusRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));

        enforceOwner(review, currentUser);

        ReviewStatus newStatus = request.getStatus();

        if (newStatus == ReviewStatus.FINAL) {
            // Must have all KPI scores filled
            for (ReviewKpiScore line : review.getKpiScores()) {
                if (line.getScore() == null) {
                    throw new IllegalArgumentException("Cannot finalize review: some KPI scores are missing");
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
                .filter(r -> !r.isArchived())
                .toList();

        List<SchemeCriticalRule> schemeCriticals = scheme.getCriticals().stream()
                .filter(r -> !r.isArchived())
                .toList();

        Map<Long, KpiPoolItem> kpiById = kpiPoolItemRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(k -> !k.isArchived())
                .collect(Collectors.toMap(KpiPoolItem::getId, Function.identity()));

        Map<Long, CriticalConditionPoolItem> criticalById = criticalPoolRepository.findByInteractionType_Id(type.getId()).stream()
                .filter(c -> !c.isArchived())
                .collect(Collectors.toMap(CriticalConditionPoolItem::getId, Function.identity()));

        // Fail fast if scheme contains missing/archived pool references (consistent error)
        for (SchemeKpiRule r : schemeKpis) {
            if (!kpiById.containsKey(r.getKpiId())) {
                throw new IllegalArgumentException("Scheme contains missing/archived KPI: " + r.getKpiId());
            }
        }
        for (SchemeCriticalRule r : schemeCriticals) {
            if (!criticalById.containsKey(r.getCriticalId())) {
                throw new IllegalArgumentException("Scheme contains missing/archived Critical: " + r.getCriticalId());
            }
        }

        return toDetailsResponse(saved, kpiById, criticalById, schemeKpis, schemeCriticals);
    }

    private void enforceOwner(Review review, User currentUser) {
        if (!Objects.equals(review.getReviewerId(), currentUser.getId())) {
            throw new AccessDeniedException("You can only edit reviews you created");
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
                .map(r -> {
                    KpiPoolItem kpi = kpiById.get(r.getKpiId());
                    if (kpi == null) {
                        throw new IllegalArgumentException("Scheme contains missing/archived KPI: " + r.getKpiId());
                    }

                    // Find review line for score (may be null)
                    Integer score = review.getKpiScores().stream()
                            .filter(l -> l.getKpi().getId().equals(kpi.getId()))
                            .findFirst()
                            .map(ReviewKpiScore::getScore)
                            .orElse(null);

                    return new ReviewDetailsResponse.KpiLine(
                            kpi.getId(),
                            kpi.getName(),
                            kpi.getWeightPercent(),
                            r.isRequired(),
                            score
                    );
                })
                .toList();

        // Critical lines in scheme order
        List<ReviewDetailsResponse.CriticalLine> criticals = schemeCriticals.stream()
                .sorted(Comparator.comparingInt(SchemeCriticalRule::getOrderIndex))
                .map(r -> {
                    CriticalConditionPoolItem c = criticalById.get(r.getCriticalId());
                    if (c == null) {
                        throw new IllegalArgumentException("Scheme contains missing/archived Critical: " + r.getCriticalId());
                    }

                    boolean triggered = review.getCriticalHits().stream()
                            .filter(h -> h.getCritical().getId().equals(c.getId()))
                            .map(ReviewCriticalHit::isTriggered)
                            .findFirst()
                            .orElse(false);

                    return new ReviewDetailsResponse.CriticalLine(
                            c.getId(),
                            c.getName(),
                            triggered
                    );
                })
                .toList();

        // totalScore: expose null if still draft with missing KPI scores
        boolean hasMissing = review.getKpiScores().stream().anyMatch(l -> l.getScore() == null);
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
