package com.mihai.overview.service;

import com.mihai.overview.entity.*;
import com.mihai.overview.exception.BadRequestException;
import com.mihai.overview.exception.ConflictException;
import com.mihai.overview.exception.ForbiddenException;
import com.mihai.overview.exception.ResourceNotFoundException;
import com.mihai.overview.repository.CriticalConditionPoolItemRepository;
import com.mihai.overview.repository.InteractionTypeRepository;
import com.mihai.overview.repository.KpiPoolItemRepository;
import com.mihai.overview.repository.SchemeRepository;
import com.mihai.overview.dto.response.SchemeDetailsResponse;
import com.mihai.overview.dto.response.SchemeListItemResponse;
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
    private final InteractionTypeRepository interactionTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SchemeListItemResponse> listSchemesForInteractionTypeCode(String interactionTypeCode) {

        if (interactionTypeCode == null || interactionTypeCode.isBlank()) {
            throw new BadRequestException("Interaction type code must not be blank");
        }

        String normalizedCode = interactionTypeCode.trim().toUpperCase();

        InteractionType interactionType = interactionTypeRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Interaction type not found: " + normalizedCode));

        if (interactionType.isArchived()) {
            throw new ForbiddenException("Interaction type is archived");
        }

        return schemeRepository.findByInteractionType_Code(normalizedCode)
                .stream()
                .filter(s -> !s.isArchived())
                .map(s -> new SchemeListItemResponse(s.getId(), s.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SchemeDetailsResponse getSchemeDetails(Long schemeId) {

        if (schemeId == null) {
            throw new BadRequestException("Scheme id must not be null");
        }

        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found: " + schemeId));

        if (scheme.isArchived()) {
            throw new ForbiddenException("Scheme is archived");
        }

        InteractionType type = scheme.getInteractionType();

        // Load pool items for this type (for fast lookup by id)
        List<KpiPoolItem> kpis = kpiPoolItemRepository.findByInteractionType_Id(type.getId());
        Map<Long, KpiPoolItem> kpiById = kpis.stream().collect(Collectors.toMap(KpiPoolItem::getId, Function.identity()));

        List<CriticalConditionPoolItem> criticals = criticalPoolRepository.findByInteractionType_Id(type.getId());
        Map<Long, CriticalConditionPoolItem> criticalById = criticals.stream().collect(Collectors.toMap(CriticalConditionPoolItem::getId, Function.identity()));

        List<SchemeDetailsResponse.KpiInScheme> kpiResponses = scheme.getKpis().stream()
                .filter(schemeKpiRule -> !schemeKpiRule.isArchived())
                .sorted(Comparator.comparingInt(SchemeKpiRule::getOrderIndex))
                .map(schemeKpiRule -> {
                    KpiPoolItem kpi = kpiById.get(schemeKpiRule.getKpiId());
                    if (kpi == null || kpi.isArchived()) {
                        // scheme points to missing/archived pool item -> treat as config error
                        throw new ConflictException("Scheme references missing or archived KPI: " + schemeKpiRule.getKpiId());
                    }
                    return new SchemeDetailsResponse.KpiInScheme(
                            kpi.getId(),
                            kpi.getName(),
                            kpi.getDescription(),
                            kpi.getDetails(),
                            kpi.getWeightPercent(),
                            schemeKpiRule.getOrderIndex(),
                            schemeKpiRule.isRequired()
                    );
                })
                .toList();

        List<SchemeDetailsResponse.CriticalInScheme> criticalResponses = scheme.getCriticals().stream()
                .filter(schemeCriticalRule -> !schemeCriticalRule.isArchived())
                .sorted(Comparator.comparingInt(SchemeCriticalRule::getOrderIndex))
                .map(schemeCriticalRule -> {
                    CriticalConditionPoolItem criticalConditionPoolItem = criticalById.get(schemeCriticalRule.getCriticalId());
                    if (criticalConditionPoolItem == null || criticalConditionPoolItem.isArchived()) {
                        throw new ConflictException("Scheme references missing or archived Critical: " + schemeCriticalRule.getCriticalId());
                    }
                    return new SchemeDetailsResponse.CriticalInScheme(
                            criticalConditionPoolItem.getId(),
                            criticalConditionPoolItem.getName(),
                            criticalConditionPoolItem.getDescription(),
                            schemeCriticalRule.getOrderIndex()
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
