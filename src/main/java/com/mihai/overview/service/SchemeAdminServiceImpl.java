package com.mihai.overview.service;

import com.mihai.overview.entity.*;
import com.mihai.overview.repository.*;
import com.mihai.overview.request.CreateSchemeRequest;
import com.mihai.overview.response.SchemeResponse;
import com.mihai.overview.util.FindAuthenticatedUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
}
