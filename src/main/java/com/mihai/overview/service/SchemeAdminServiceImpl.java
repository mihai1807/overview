package com.mihai.overview.service;

import com.mihai.overview.entity.*;
import com.mihai.overview.repository.*;
import com.mihai.overview.request.CreateSchemeRequest;
import com.mihai.overview.response.SchemeListItemStatusResponse;
import com.mihai.overview.response.SchemeResponse;
import com.mihai.overview.util.FindAuthenticatedUser;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("InteractionType code is mandatory");
        }

        String normalizedCode = code.trim().toUpperCase();

        InteractionType type = interactionTypeRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("InteractionType not found: " + normalizedCode));

        if (type.isArchived()) {
            throw new IllegalArgumentException("InteractionType is archived");
        }

        // --- KPIs ---
        List<CreateSchemeRequest.SchemeKpiInput> kpiInputs =
                request.getKpis() == null ? Collections.emptyList() : request.getKpis();

        Set<Long> kpiIds = new HashSet<>();
        int weightSum = 0;

        List<SchemeKpiRule> schemeKpis = new ArrayList<>();
        for (CreateSchemeRequest.SchemeKpiInput in : kpiInputs) {
            if (in == null) {
                throw new IllegalArgumentException("KPI input cannot be null");
            }
            if (in.getKpiId() == null) {
                throw new IllegalArgumentException("KPI id is mandatory");
            }

            if (!kpiIds.add(in.getKpiId())) {
                throw new IllegalArgumentException("Duplicate KPI id in scheme: " + in.getKpiId());
            }

            KpiPoolItem kpi = kpiPoolItemRepository.findById(in.getKpiId())
                    .orElseThrow(() -> new IllegalArgumentException("KPI not found: " + in.getKpiId()));

            if (kpi.isArchived()) {
                throw new IllegalArgumentException("KPI is archived: " + kpi.getId());
            }
            if (!kpi.getInteractionType().getId().equals(type.getId())) {
                throw new IllegalArgumentException("KPI does not belong to scheme InteractionType: " + kpi.getId());
            }

            if (kpi.getWeightPercent() < 0 || kpi.getWeightPercent() > 100) {
                throw new IllegalArgumentException("Invalid KPI weightPercent for KPI: " + kpi.getId());
            }

            weightSum += kpi.getWeightPercent();
            schemeKpis.add(new SchemeKpiRule(kpi.getId(), in.getOrderIndex(), in.isRequired(), false));
        }

        if (weightSum != 100) {
            throw new IllegalArgumentException("Scheme KPI weights must sum to 100. Current: " + weightSum);
        }

        // --- Criticals ---
        List<CreateSchemeRequest.SchemeCriticalInput> criticalInputs =
                request.getCriticals() == null ? Collections.emptyList() : request.getCriticals();

        Set<Long> criticalIds = new HashSet<>();
        List<SchemeCriticalRule> schemeCriticals = new ArrayList<>();
        for (CreateSchemeRequest.SchemeCriticalInput in : criticalInputs) {
            if (in == null) {
                throw new IllegalArgumentException("Critical input cannot be null");
            }
            if (in.getCriticalId() == null) {
                throw new IllegalArgumentException("Critical id is mandatory");
            }

            if (!criticalIds.add(in.getCriticalId())) {
                throw new IllegalArgumentException("Duplicate critical id in scheme: " + in.getCriticalId());
            }

            CriticalConditionPoolItem c = criticalPoolRepository.findById(in.getCriticalId())
                    .orElseThrow(() -> new IllegalArgumentException("Critical not found: " + in.getCriticalId()));

            if (c.isArchived()) {
                throw new IllegalArgumentException("Critical is archived: " + c.getId());
            }
            if (!c.getInteractionType().getId().equals(type.getId())) {
                throw new IllegalArgumentException("Critical does not belong to scheme InteractionType: " + c.getId());
            }

            schemeCriticals.add(new SchemeCriticalRule(c.getId(), in.getOrderIndex(), false));
        }

        // --- Create scheme ---
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Scheme name is mandatory");
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
            throw new IllegalArgumentException("InteractionType code is mandatory");
        }

        String normalizedCode = code.trim().toUpperCase();

        // production-grade: unknown interaction type -> 404
        InteractionType type = interactionTypeRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "InteractionType not found: " + normalizedCode
                ));

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

        // Requirement: for NEW reviews, scheme must have no archived rules
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
                .filter(r -> !r.isArchived())
                .toList();

        int weightSum = 0;
        for (SchemeKpiRule r : kpiRules) {
            KpiPoolItem kpi = activeKpisById.get(r.getKpiId());
            if (kpi == null) {
                return false;
            }
            weightSum += kpi.getWeightPercent();
        }
        if (weightSum != 100) {
            return false;
        }

        for (SchemeCriticalRule r : criticalRules) {
            CriticalConditionPoolItem c = activeCriticalsById.get(r.getCriticalId());
            if (c == null) {
                return false;
            }
        }

        return true;
    }

    @Override
    @Transactional
    public void archiveScheme(Long schemeId) {
        if (schemeId == null || schemeId < 1) {
            throw new IllegalArgumentException("schemeId must be positive");
        }

        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        if (scheme.isArchived()) {
            return; // idempotent
        }

        scheme.setArchived(true);
        schemeRepository.save(scheme);
    }

    @Override
    @Transactional
    public void unarchiveScheme(Long schemeId) {
        if (schemeId == null || schemeId < 1) {
            throw new IllegalArgumentException("schemeId must be positive");
        }

        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        if (!scheme.isArchived()) {
            return; // idempotent
        }

        scheme.setArchived(false);
        schemeRepository.save(scheme);
    }

}
