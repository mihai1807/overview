package com.mihai.overview.service;

import com.mihai.overview.entity.*;
import com.mihai.overview.exception.BadRequestException;
import com.mihai.overview.exception.ConflictException;
import com.mihai.overview.exception.ResourceNotFoundException;
import com.mihai.overview.repository.*;
import com.mihai.overview.dto.request.CreateSchemeRequest;
import com.mihai.overview.dto.response.SchemeDetailsResponse;
import com.mihai.overview.dto.response.SchemeListItemStatusResponse;
import com.mihai.overview.dto.response.SchemeResponse;
import com.mihai.overview.security.FindAuthenticatedUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class SchemeAdminServiceImpl implements SchemeAdminService {

    private final InteractionTypeRepository interactionTypeRepository;
    private final KpiPoolItemRepository kpiPoolItemRepository;
    private final CriticalConditionPoolItemRepository criticalPoolRepository;
    private final SchemeRepository schemeRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;

    @Override
    @Transactional
    public SchemeResponse createScheme(String code, CreateSchemeRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        if (request == null) {
            throw new BadRequestException("Request body is mandatory");
        }

        if (code == null || code.trim().isEmpty()) {
            throw new BadRequestException("InteractionType code is mandatory");
        }

        String normalizedCode = code.trim().toUpperCase();

        InteractionType type = interactionTypeRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("InteractionType not found: " + normalizedCode));

        if (type.isArchived()) {
            throw new ConflictException("InteractionType is archived");
        }

        // --- KPIs ---
        List<CreateSchemeRequest.SchemeKpiInput> kpiInputs =
                request.getKpis() == null ? Collections.emptyList() : request.getKpis();

        Set<Long> kpiIds = new HashSet<>();
        int weightSum = 0;

        List<SchemeKpiRule> schemeKpis = new ArrayList<>();
        for (CreateSchemeRequest.SchemeKpiInput kpiInput : kpiInputs) {
            if (kpiInput == null) {
                throw new BadRequestException("KPI input cannot be null");
            }
            if (kpiInput.getKpiId() == null) {
                throw new BadRequestException("KPI id is mandatory");
            }

            if (!kpiIds.add(kpiInput.getKpiId())) {
                throw new BadRequestException("Duplicate KPI id in scheme: " + kpiInput.getKpiId());
            }

            KpiPoolItem kpi = kpiPoolItemRepository.findById(kpiInput.getKpiId())
                    .orElseThrow(() -> new ResourceNotFoundException("KPI not found: " + kpiInput.getKpiId()));

            if (kpi.isArchived()) {
                throw new ConflictException("KPI is archived: " + kpi.getId());
            }
            if (!kpi.getInteractionType().getId().equals(type.getId())) {
                throw new BadRequestException("KPI does not belong to scheme InteractionType: " + kpi.getId());
            }

            if (kpi.getWeightPercent() < 0 || kpi.getWeightPercent() > 100) {
                throw new BadRequestException("Invalid KPI weightPercent for KPI: " + kpi.getId());
            }

            weightSum += kpi.getWeightPercent();
            schemeKpis.add(new SchemeKpiRule(kpi.getId(), kpiInput.getOrderIndex(), kpiInput.isRequired(), false));
        }

        if (weightSum != 100) {
            throw new BadRequestException("Scheme KPI weights must sum to 100. Current: " + weightSum);
        }

        // --- Criticals ---
        List<CreateSchemeRequest.SchemeCriticalInput> criticalInputs =
                request.getCriticals() == null ? Collections.emptyList() : request.getCriticals();

        Set<Long> criticalIds = new HashSet<>();
        List<SchemeCriticalRule> schemeCriticals = new ArrayList<>();
        for (CreateSchemeRequest.SchemeCriticalInput criticalInput : criticalInputs) {
            if (criticalInput == null) {
                throw new BadRequestException("Critical input cannot be null");
            }
            if (criticalInput.getCriticalId() == null) {
                throw new BadRequestException("Critical id is mandatory");
            }

            if (!criticalIds.add(criticalInput.getCriticalId())) {
                throw new BadRequestException("Duplicate critical id in scheme: " + criticalInput.getCriticalId());
            }

            CriticalConditionPoolItem conditionPoolItem = criticalPoolRepository.findById(criticalInput.getCriticalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Critical not found: " + criticalInput.getCriticalId()));

            if (conditionPoolItem.isArchived()) {
                throw new ConflictException("Critical is archived: " + conditionPoolItem.getId());
            }
            if (!conditionPoolItem.getInteractionType().getId().equals(type.getId())) {
                throw new BadRequestException("Critical does not belong to scheme InteractionType: " + conditionPoolItem.getId());
            }

            schemeCriticals.add(new SchemeCriticalRule(conditionPoolItem.getId(), criticalInput.getOrderIndex(), false));
        }

        // --- Create scheme ---
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Scheme name is mandatory");
        }

        Scheme scheme = new Scheme();
        scheme.setInteractionType(type);
        scheme.setName(request.getName().trim());
        scheme.setCreatedByUserId(currentUser.getId());
        scheme.setArchived(false);
        scheme.setKpis(schemeKpis);
        scheme.setCriticals(schemeCriticals);

        Scheme saved = schemeRepository.save(scheme);

        return new SchemeResponse(saved.getId(), type.getId(), saved.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchemeListItemStatusResponse> listSchemesByInteractionType(String code, boolean includeArchived) {
        if (code == null || code.trim().isEmpty()) {
            throw new BadRequestException("InteractionType code is mandatory");
        }

        String normalizedCode = code.trim().toUpperCase();

        InteractionType type = interactionTypeRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("InteractionType not found: " + normalizedCode));

        List<Scheme> schemes = schemeRepository.findByInteractionType_Code(normalizedCode);

        if (!includeArchived) {
            schemes = schemes.stream()
                    .filter(s -> !s.isArchived())
                    .toList();
        }

        if (schemes.isEmpty()) {
            return List.of();
        }

        Long interactionTypeId = type.getId();

        Map<Long, KpiPoolItem> activeKpisById = kpiPoolItemRepository.findByInteractionType_Id(interactionTypeId).stream()
                .filter(k -> !k.isArchived())
                .collect(Collectors.toMap(KpiPoolItem::getId, Function.identity()));

        Map<Long, CriticalConditionPoolItem> activeCriticalsById = criticalPoolRepository.findByInteractionType_Id(interactionTypeId).stream()
                .filter(c -> !c.isArchived())
                .collect(Collectors.toMap(CriticalConditionPoolItem::getId, Function.identity()));

        return schemes.stream()
                .sorted(Comparator.comparing(Scheme::getName, String.CASE_INSENSITIVE_ORDER))
                .map(s -> new SchemeListItemStatusResponse(
                        s.getId(),
                        s.getName(),
                        s.isArchived(),
                        computeUsableForNewReviews(s, activeKpisById, activeCriticalsById)
                ))
                .toList();
    }

    private boolean computeUsableForNewReviews(
            Scheme scheme,
            Map<Long, KpiPoolItem> activeKpisById,
            Map<Long, CriticalConditionPoolItem> activeCriticalsById
    ) {
        if (scheme.isArchived()) {
            return false;
        }

        boolean hasAnyArchivedRule =
                scheme.getKpis().stream().anyMatch(SchemeKpiRule::isArchived) ||
                        scheme.getCriticals().stream().anyMatch(SchemeCriticalRule::isArchived);

        if (hasAnyArchivedRule) {
            return false;
        }

        List<SchemeKpiRule> kpiRules = scheme.getKpis().stream()
                .filter(r -> !r.isArchived())
                .toList();

        List<SchemeCriticalRule> criticalRules = scheme.getCriticals().stream()
                .filter(schemeCriticalRule -> !schemeCriticalRule.isArchived())
                .toList();

        int weightSum = 0;
        for (SchemeKpiRule schemeKpiRule : kpiRules) {
            KpiPoolItem kpi = activeKpisById.get(schemeKpiRule.getKpiId());
            if (kpi == null) {
                return false;
            }
            weightSum += kpi.getWeightPercent();
        }
        if (weightSum != 100) {
            return false;
        }

        for (SchemeCriticalRule schemeCriticalRule : criticalRules) {
            CriticalConditionPoolItem criticalConditionPoolItem = activeCriticalsById.get(schemeCriticalRule.getCriticalId());
            if (criticalConditionPoolItem == null) {
                return false;
            }
        }

        return true;
    }


    @Override
    @Transactional(readOnly = true)
    public SchemeDetailsResponse getSchemeDetails(Long schemeId, boolean includeArchived) {
        if (schemeId == null || schemeId < 1) {
            throw new BadRequestException("schemeId must be positive");
        }

        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found: " + schemeId));

        if (scheme.isArchived() && !includeArchived) {
            throw new ConflictException("Scheme is archived");
        }

        InteractionType type = scheme.getInteractionType();

        List<KpiPoolItem> kpis = kpiPoolItemRepository.findByInteractionType_Id(type.getId());
        Map<Long, KpiPoolItem> kpiById = kpis.stream()
                .collect(Collectors.toMap(KpiPoolItem::getId, Function.identity()));

        List<CriticalConditionPoolItem> criticals = criticalPoolRepository.findByInteractionType_Id(type.getId());
        Map<Long, CriticalConditionPoolItem> criticalById = criticals.stream()
                .collect(Collectors.toMap(CriticalConditionPoolItem::getId, Function.identity()));

        Stream<SchemeKpiRule> kpiRuleStream = scheme.getKpis().stream();
        if (!includeArchived) {
            kpiRuleStream = kpiRuleStream.filter(r -> !r.isArchived());
        }

        List<SchemeDetailsResponse.KpiInScheme> kpiResponses = kpiRuleStream
                .sorted(Comparator.comparingInt(SchemeKpiRule::getOrderIndex))
                .map(schemeKpiRule -> {
                    KpiPoolItem kpi = kpiById.get(schemeKpiRule.getKpiId());
                    if (kpi == null || kpi.isArchived()) {
                        throw new ConflictException("Scheme references missing/archived KPI: " + schemeKpiRule.getKpiId());
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

        Stream<SchemeCriticalRule> criticalRuleStream = scheme.getCriticals().stream();
        if (!includeArchived) {
            criticalRuleStream = criticalRuleStream.filter(r -> !r.isArchived());
        }

        List<SchemeDetailsResponse.CriticalInScheme> criticalResponses = criticalRuleStream
                .sorted(Comparator.comparingInt(SchemeCriticalRule::getOrderIndex))
                .map(schemeCriticalRule -> {
                    CriticalConditionPoolItem c = criticalById.get(schemeCriticalRule.getCriticalId());
                    if (c == null || c.isArchived()) {
                        throw new ConflictException("Scheme references missing/archived Critical: " + schemeCriticalRule.getCriticalId());
                    }
                    return new SchemeDetailsResponse.CriticalInScheme(
                            c.getId(),
                            c.getName(),
                            c.getDescription(),
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

    @Override
    @Transactional
    public void archiveScheme(Long schemeId) {
        if (schemeId == null || schemeId < 1) {
            throw new BadRequestException("schemeId must be positive");
        }

        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found: " + schemeId));

        if (scheme.isArchived()) {
            throw new ConflictException("Scheme is already archived");
        }

        scheme.setArchived(true);
        schemeRepository.save(scheme);
    }

    @Override
    @Transactional
    public void unarchiveScheme(Long schemeId) {
        if (schemeId == null || schemeId < 1) {
            throw new BadRequestException("schemeId must be positive");
        }

        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found: " + schemeId));

        if (!scheme.isArchived()) {
            throw new ConflictException("Scheme is already active");
        }

        scheme.setArchived(false);
        schemeRepository.save(scheme);
    }
}