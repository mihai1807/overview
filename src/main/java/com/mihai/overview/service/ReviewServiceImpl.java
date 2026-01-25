package com.mihai.overview.service;

import com.mihai.overview.entity.*;
import com.mihai.overview.repository.*;
import com.mihai.overview.request.CreateReviewRequest;
import com.mihai.overview.response.ReviewResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {

        Scheme scheme = schemeRepository.findById(request.getSchemeId())
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + request.getSchemeId()));

        if (scheme.isArchived()) {
            throw new IllegalArgumentException("Scheme is archived");
        }

        InteractionType type = scheme.getInteractionType();

        // Build allowed KPI ids from scheme rules
        Set<Long> allowedKpiIds = scheme.getKpis().stream()
                .filter(r -> !r.isArchived())
                .map(SchemeKpiRule::getKpiId)
                .collect(Collectors.toSet());

        // Build allowed Critical ids from scheme rules
        Set<Long> allowedCriticalIds = scheme.getCriticals().stream()
                .filter(r -> !r.isArchived())
                .map(SchemeCriticalRule::getCriticalId)
                .collect(Collectors.toSet());

        // Validate provided KPI scores: must match scheme membership, no duplicates
        Set<Long> seenKpis = new HashSet<>();
        for (CreateReviewRequest.KpiScoreInput s : request.getKpiScores()) {
            if (!seenKpis.add(s.getKpiId())) {
                throw new IllegalArgumentException("Duplicate KPI score entry: " + s.getKpiId());
            }
            if (!allowedKpiIds.contains(s.getKpiId())) {
                throw new IllegalArgumentException("KPI not part of scheme: " + s.getKpiId());
            }
        }

        // Validate provided critical hits: must match scheme membership, no duplicates
        Set<Long> seenCriticals = new HashSet<>();
        for (CreateReviewRequest.CriticalHitInput h : request.getCriticalHits()) {
            if (!seenCriticals.add(h.getCriticalId())) {
                throw new IllegalArgumentException("Duplicate critical entry: " + h.getCriticalId());
            }
            if (!allowedCriticalIds.contains(h.getCriticalId())) {
                throw new IllegalArgumentException("Critical not part of scheme: " + h.getCriticalId());
            }
        }

        // Load pool KPIs for this type and map by id (weights come from pool)
        List<KpiPoolItem> poolKpis = kpiPoolItemRepository.findByInteractionType_Id(type.getId());
        Map<Long, KpiPoolItem> kpiById = poolKpis.stream()
                .filter(k -> !k.isArchived())
                .collect(Collectors.toMap(KpiPoolItem::getId, Function.identity()));

        // Compute total score using: sum(score * weight) / 100
        int weightedSum = 0;
        int weightSum = 0;

        for (CreateReviewRequest.KpiScoreInput s : request.getKpiScores()) {
            KpiPoolItem kpi = kpiById.get(s.getKpiId());
            if (kpi == null) {
                throw new IllegalStateException("Scheme references KPI not found/archived in pool: " + s.getKpiId());
            }
            int w = kpi.getWeightPercent();
            weightedSum += s.getScore() * w;
            weightSum += w;
        }

        // Since scheme creation enforced weights == 100, this should be 100 if reviewer scored all scheme KPIs.
        // If you allow partial scoring later, this logic will need adjustment.
        if (weightSum != 100) {
            throw new IllegalArgumentException("Review must include KPI scores whose weights sum to 100. Current: " + weightSum);
        }

        int totalScore = Math.round(weightedSum / 100.0f);
        String periodKey = request.getOccurredAt().format(PERIOD_FMT);

        Review review = new Review();
        review.setScheme(scheme);
        review.setInteractionType(type);
        review.setReviewedUserId(request.getReviewedUserId());
        review.setReviewerId(request.getReviewerId());
        review.setTicketId(request.getTicketId());
        review.setCid(request.getCid());
        review.setOccurredAt(request.getOccurredAt());
        review.setPeriodKey(periodKey);
        review.setTotalScore(totalScore);
        review.setAccepted(true);

        // Attach KPI scores
        for (CreateReviewRequest.KpiScoreInput s : request.getKpiScores()) {
            ReviewKpiScore r = new ReviewKpiScore();
            r.setReview(review);
            r.setKpi(kpiById.get(s.getKpiId()));
            r.setScore(s.getScore());
            review.getKpiScores().add(r);
        }

        // Attach critical hits
        // Load critical pool map for type
        List<CriticalConditionPoolItem> poolCriticals = criticalPoolRepository.findByInteractionType_Id(type.getId());
        Map<Long, CriticalConditionPoolItem> criticalById = poolCriticals.stream()
                .filter(c -> !c.isArchived())
                .collect(Collectors.toMap(CriticalConditionPoolItem::getId, Function.identity()));

        for (CreateReviewRequest.CriticalHitInput h : request.getCriticalHits()) {
            CriticalConditionPoolItem c = criticalById.get(h.getCriticalId());
            if (c == null) {
                throw new IllegalStateException("Scheme references Critical not found/archived in pool: " + h.getCriticalId());
            }
            ReviewCriticalHit hit = new ReviewCriticalHit();
            hit.setReview(review);
            hit.setCritical(c);
            hit.setTriggered(h.isTriggered());
            review.getCriticalHits().add(hit);
        }

        Review saved = reviewRepository.save(review);
        return new ReviewResponse(saved.getId(), scheme.getId(), saved.getPeriodKey(), saved.getTotalScore());
    }
}
