package com.mihai.overview.service;

import com.mihai.overview.entity.InteractionKpi;
import com.mihai.overview.entity.KpiScheme;
import com.mihai.overview.entity.KpiSchemeItem;
import com.mihai.overview.entity.ReviewType;
import com.mihai.overview.repository.InteractionKpiRepository;
import com.mihai.overview.repository.KpiSchemeItemRepository;
import com.mihai.overview.repository.KpiSchemeRepository;
import com.mihai.overview.repository.ReviewTypeRepository;
import com.mihai.overview.request.AddKpiSchemeItemRequest;
import com.mihai.overview.request.CreateInteractionKpiRequest;
import com.mihai.overview.request.CreateKpiSchemeRequest;
import com.mihai.overview.request.CreateReviewTypeRequest;
import com.mihai.overview.response.InteractionKpiResponse;
import com.mihai.overview.response.KpiSchemeItemResponse;
import com.mihai.overview.response.KpiSchemeResponse;
import com.mihai.overview.response.ReviewTypeResponse;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class ReviewAdminServiceImpl implements ReviewAdminService {

    private final ReviewTypeRepository reviewTypeRepository;
    private final InteractionKpiRepository interactionKpiRepository;
    private final KpiSchemeRepository kpiSchemeRepository;
    private final KpiSchemeItemRepository kpiSchemeItemRepository;

    @Override
    @Transactional
    public ReviewTypeResponse createReviewType(CreateReviewTypeRequest request) {
        ReviewType type = new ReviewType();
        type.setCode(request.getCode().trim().toUpperCase());
        type.setName(request.getName().trim());

        ReviewType saved = reviewTypeRepository.save(type);

        return new ReviewTypeResponse(saved.getId(), saved.getCode(), saved.getName(),
                saved.getActiveScheme() == null ? null : saved.getActiveScheme().getId());
    }

    @Override
    @Transactional
    public InteractionKpiResponse createKpiUnderReviewType(Long reviewTypeId, CreateInteractionKpiRequest request) {
        ReviewType type = reviewTypeRepository.findById(reviewTypeId)
                .orElseThrow(() -> new IllegalArgumentException("ReviewType not found: " + reviewTypeId));

        InteractionKpi kpi = new InteractionKpi();
        kpi.setReviewType(type);
        kpi.setName(request.getName().trim());
        kpi.setDescription(request.getDescription().trim());
        kpi.setActive(true);

        InteractionKpi saved = interactionKpiRepository.save(kpi);

        return new InteractionKpiResponse(saved.getId(), type.getId(), saved.getName(), saved.getDescription(), saved.isActive());
    }

    @Override
    @Transactional
    public KpiSchemeResponse createSchemeUnderReviewType(Long reviewTypeId, CreateKpiSchemeRequest request) {
        ReviewType type = reviewTypeRepository.findById(reviewTypeId)
                .orElseThrow(() -> new IllegalArgumentException("ReviewType not found: " + reviewTypeId));

        KpiScheme scheme = new KpiScheme();
        scheme.setReviewType(type);
        scheme.setName(request.getName().trim());
        scheme.setActive(false);

        KpiScheme saved = kpiSchemeRepository.save(scheme);

        return new KpiSchemeResponse(saved.getId(), type.getId(), saved.getName(), saved.isActive());
    }

    @Override
    @Transactional
    public KpiSchemeItemResponse addItemToScheme(Long schemeId, AddKpiSchemeItemRequest request) {
        KpiScheme scheme = kpiSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        InteractionKpi kpi = interactionKpiRepository.findById(request.getKpiId())
                .orElseThrow(() -> new IllegalArgumentException("KPI not found: " + request.getKpiId()));

        // Enforce: KPI must belong to the same ReviewType as the scheme
        if (!kpi.getReviewType().getId().equals(scheme.getReviewType().getId())) {
            throw new IllegalArgumentException("KPI does not belong to the scheme's ReviewType");
        }

        if (request.getMaxScore() <= request.getMinScore()) {
            throw new IllegalArgumentException("maxScore must be greater than minScore");
        }

        // Enforce: scheme weights cannot exceed 100 while building it
        List<KpiSchemeItem> existing = kpiSchemeItemRepository.findByScheme_Id(schemeId);
        int currentWeight = existing.stream().mapToInt(KpiSchemeItem::getWeightPercent).sum();
        if (currentWeight + request.getWeightPercent() > 100) {
            throw new IllegalArgumentException("Total scheme weight cannot exceed 100%. Current: " + currentWeight);
        }

        KpiSchemeItem item = new KpiSchemeItem();
        item.setScheme(scheme);
        item.setKpi(kpi);
        item.setMinScore(request.getMinScore());
        item.setMaxScore(request.getMaxScore());
        item.setWeightPercent(request.getWeightPercent());
        item.setOrderIndex(request.getOrderIndex());
        item.setRequired(request.isRequired());

        try {
            KpiSchemeItem saved = kpiSchemeItemRepository.save(item);
            return new KpiSchemeItemResponse(
                    saved.getId(),
                    scheme.getId(),
                    kpi.getId(),
                    kpi.getName(),
                    saved.getMinScore(),
                    saved.getMaxScore(),
                    saved.getWeightPercent(),
                    saved.getOrderIndex(),
                    saved.isRequired()
            );
        } catch (DataIntegrityViolationException ex) {
            // unique constraint (scheme_id, kpi_id)
            throw new IllegalArgumentException("This KPI is already added to the scheme");
        }
    }

    @Override
    @Transactional
    public ReviewTypeResponse activateSchemeForReviewType(Long reviewTypeId, Long schemeId) {
        ReviewType type = reviewTypeRepository.findById(reviewTypeId)
                .orElseThrow(() -> new IllegalArgumentException("ReviewType not found: " + reviewTypeId));

        KpiScheme scheme = kpiSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        if (!scheme.getReviewType().getId().equals(type.getId())) {
            throw new IllegalArgumentException("Scheme does not belong to the given ReviewType");
        }

        List<KpiSchemeItem> items = kpiSchemeItemRepository.findByScheme_Id(schemeId);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Cannot activate a scheme with no KPI items");
        }

        int totalWeight = items.stream().mapToInt(KpiSchemeItem::getWeightPercent).sum();
        if (totalWeight != 100) {
            throw new IllegalArgumentException("Cannot activate scheme: total weights must be 100%. Current: " + totalWeight);
        }

        // Deactivate all schemes for this review type, then activate the chosen one
        List<KpiScheme> allSchemes = kpiSchemeRepository.findByReviewType_Id(type.getId());
        for (KpiScheme s : allSchemes) {
            s.setActive(false);
        }
        scheme.setActive(true);
        kpiSchemeRepository.saveAll(allSchemes);

        type.setActiveScheme(scheme);
        ReviewType savedType = reviewTypeRepository.save(type);

        return new ReviewTypeResponse(savedType.getId(), savedType.getCode(), savedType.getName(),
                savedType.getActiveScheme() == null ? null : savedType.getActiveScheme().getId());
    }
}
