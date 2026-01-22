package com.mihai.overview.service;

import com.mihai.overview.entity.KpiScheme;
import com.mihai.overview.entity.KpiSchemeItem;
import com.mihai.overview.entity.ReviewType;
import com.mihai.overview.repository.KpiSchemeItemRepository;
import com.mihai.overview.repository.ReviewTypeRepository;
import com.mihai.overview.response.ActiveSchemeResponse;
import com.mihai.overview.response.SchemeItemResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@AllArgsConstructor
public class ReviewConfigServiceImpl implements ReviewConfigService {

    private final ReviewTypeRepository reviewTypeRepository;
    private final KpiSchemeItemRepository kpiSchemeItemRepository;

    @Override
    @Transactional(readOnly = true)
    public ActiveSchemeResponse getActiveSchemeForReviewTypeCode(String reviewTypeCode) {
        ReviewType type = reviewTypeRepository.findByCode(reviewTypeCode)
                .orElseThrow(() -> new IllegalArgumentException("ReviewType not found: " + reviewTypeCode));

        KpiScheme active = type.getActiveScheme();
        if (active == null) {
            throw new IllegalStateException("No active KPI scheme configured for ReviewType: " + reviewTypeCode);
        }

        if (active.isArchived()) {
            throw new IllegalStateException("Active scheme is archived for ReviewType: " + reviewTypeCode);
        }

        List<KpiSchemeItem> items = kpiSchemeItemRepository.findByScheme_Id(active.getId())
                .stream()
                .filter(i -> !i.isArchived())
                .toList();

        List<SchemeItemResponse> itemResponses = items.stream()
                .sorted(Comparator.comparingInt(KpiSchemeItem::getOrderIndex))
                .map(i -> new SchemeItemResponse(
                        i.getId(),
                        i.getKpi().getId(),
                        i.getKpi().getName(),
                        i.getKpi().getDescription(),
                        i.getMinScore(),
                        i.getMaxScore(),
                        i.getWeightPercent(),
                        i.getOrderIndex(),
                        i.isRequired()
                ))
                .toList();

        return new ActiveSchemeResponse(active.getId(), active.getName(), type.getCode(), itemResponses);
    }
}
