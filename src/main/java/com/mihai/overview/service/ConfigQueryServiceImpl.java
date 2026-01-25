package com.mihai.overview.service;

import com.mihai.overview.entity.*;
import com.mihai.overview.repository.CriticalConditionPoolItemRepository;
import com.mihai.overview.repository.KpiPoolItemRepository;
import com.mihai.overview.repository.SchemeRepository;
import com.mihai.overview.response.SchemeDetailsResponse;
import com.mihai.overview.response.SchemeListItemResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ConfigQueryServiceImpl implements ConfigQueryService {

    private final SchemeRepository schemeRepository;
    private final KpiPoolItemRepository kpiPoolItemRepository;
    private final CriticalConditionPoolItemRepository criticalPoolRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SchemeListItemResponse> listSchemesForInteractionTypeCode(String interactionTypeCode) {
        return schemeRepository.findByInteractionType_Code(interactionTypeCode.trim().toUpperCase())
                .stream()
                .filter(s -> !s.isArchived())
                .map(s -> new SchemeListItemResponse(s.getId(), s.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SchemeDetailsResponse getSchemeDetails(Long schemeId) {
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        if (scheme.isArchived()) {
            throw new IllegalArgumentException("Scheme is archived");
        }

        InteractionType type = scheme.getInteractionType();

        // Load pool items for this type (for fast lookup by id)
        List<KpiPoolItem> kpis = kpiPoolItemRepository.findByInteractionType_Id(type.getId());
        Map<Long, KpiPoolItem> kpiById = kpis.stream().collect(Collectors.toMap(KpiPoolItem::getId, Function.identity()));

        List<CriticalConditionPoolItem> criticals = criticalPoolRepository.findByInteractionType_Id(type.getId());
        Map<Long, CriticalConditionPoolItem> criticalById = criticals.stream().collect(Collectors.toMap(CriticalConditionPoolItem::getId, Function.identity()));

        var kpiResponses = scheme.getKpis().stream()
                .filter(r -> !r.isArchived())
                .sorted(Comparator.comparingInt(SchemeKpiRule::getOrderIndex))
                .map(r -> {
                    KpiPoolItem kpi = kpiById.get(r.getKpiId());
                    if (kpi == null || kpi.isArchived()) {
                        // scheme points to missing/archived pool item -> treat as config error
                        throw new IllegalStateException("Scheme references missing/archived KPI: " + r.getKpiId());
                    }
                    return new SchemeDetailsResponse.KpiInScheme(
                            kpi.getId(),
                            kpi.getName(),
                            kpi.getDescription(),
                            kpi.getDetails(),
                            kpi.getWeightPercent(),
                            r.getOrderIndex(),
                            r.isRequired()
                    );
                })
                .toList();

        var criticalResponses = scheme.getCriticals().stream()
                .filter(r -> !r.isArchived())
                .sorted(Comparator.comparingInt(SchemeCriticalRule::getOrderIndex))
                .map(r -> {
                    CriticalConditionPoolItem c = criticalById.get(r.getCriticalId());
                    if (c == null || c.isArchived()) {
                        throw new IllegalStateException("Scheme references missing/archived Critical: " + r.getCriticalId());
                    }
                    return new SchemeDetailsResponse.CriticalInScheme(
                            c.getId(),
                            c.getName(),
                            c.getDescription(),
                            r.getOrderIndex()
                    );
                })
                .toList();

        return new SchemeDetailsResponse(
                scheme.getId(),
                scheme.getName(),
                type.getCode(),
                kpiResponses,
                criticalResponses
        );
    }
}
