package com.mihai.overview.service;

import com.mihai.overview.entity.CriticalConditionPoolItem;
import com.mihai.overview.entity.InteractionType;
import com.mihai.overview.entity.KpiPoolItem;
import com.mihai.overview.entity.User;
import com.mihai.overview.repository.CriticalConditionPoolItemRepository;
import com.mihai.overview.repository.InteractionTypeRepository;
import com.mihai.overview.repository.KpiPoolItemRepository;
import com.mihai.overview.request.CreateCriticalConditionPoolItemRequest;
import com.mihai.overview.request.CreateInteractionTypeRequest;
import com.mihai.overview.request.CreateKpiPoolItemRequest;
import com.mihai.overview.response.CriticalConditionPoolItemResponse;
import com.mihai.overview.response.InteractionTypeResponse;
import com.mihai.overview.response.KpiPoolItemResponse;
import com.mihai.overview.util.FindAuthenticatedUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AdminConfigServiceImpl implements AdminConfigService {

    private final InteractionTypeRepository interactionTypeRepository;
    private final KpiPoolItemRepository kpiPoolItemRepository;
    private final CriticalConditionPoolItemRepository criticalPoolRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;

    @Override
    @Transactional
    public InteractionTypeResponse createInteractionType(CreateInteractionTypeRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        InteractionType type = new InteractionType();
        type.setCode(request.getCode().trim().toUpperCase());
        type.setName(request.getName().trim());
        type.setCreatedByUserId(currentUser.getId());
        type.setArchived(false);

        InteractionType saved = interactionTypeRepository.save(type);
        return new InteractionTypeResponse(saved.getId(), saved.getCode(), saved.getName());
    }

    @Override
    @Transactional
    public KpiPoolItemResponse createKpiPoolItem(String interactionTypeCode, CreateKpiPoolItemRequest request) {
        User currentUser = find_authenticated();

        String code = interactionTypeCode.trim().toUpperCase();

        InteractionType type = interactionTypeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("InteractionType not found: " + code));


        if (type.isArchived()) {
            throw new IllegalArgumentException("InteractionType is archived");
        }

        if (request.getWeightPercent() < 0 || request.getWeightPercent() > 100) {
            throw new IllegalArgumentException("weightPercent must be between 0 and 100");
        }

        KpiPoolItem kpi = new KpiPoolItem();
        kpi.setInteractionType(type);
        kpi.setName(request.getName().trim());
        kpi.setDescription(request.getDescription().trim());
        kpi.setDetails(request.getDetails().trim());
        kpi.setWeightPercent(request.getWeightPercent());
        kpi.setCreatedByUserId(currentUser.getId());
        kpi.setArchived(false);

        KpiPoolItem saved = kpiPoolItemRepository.save(kpi);
        return new KpiPoolItemResponse(
                saved.getId(),
                type.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.getDetails(),
                saved.getWeightPercent()
        );
    }

    @Override
    @Transactional
    public CriticalConditionPoolItemResponse createCriticalConditionPoolItem(String interactionTypeCode, CreateCriticalConditionPoolItemRequest request) {
        User currentUser = find_authenticated();

        String code = interactionTypeCode.trim().toUpperCase();

        InteractionType type = interactionTypeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("InteractionType not found: " + code));

        if (type.isArchived()) {
            throw new IllegalArgumentException("InteractionType is archived");
        }

        CriticalConditionPoolItem c = new CriticalConditionPoolItem();
        c.setInteractionType(type);
        c.setName(request.getName().trim());
        c.setDescription(request.getDescription().trim());
        c.setCreatedByUserId(currentUser.getId());
        c.setArchived(false);

        CriticalConditionPoolItem saved = criticalPoolRepository.save(c);
        return new CriticalConditionPoolItemResponse(saved.getId(), type.getId(), saved.getName(), saved.getDescription());
    }

    private User find_authenticated() {
        return findAuthenticatedUser.getAuthenticatedUser();
    }
}
