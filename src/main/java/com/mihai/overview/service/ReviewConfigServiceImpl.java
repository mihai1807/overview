package com.mihai.overview.service;

import com.mihai.overview.entity.CriticalConditionPoolItem;
import com.mihai.overview.entity.InteractionType;
import com.mihai.overview.entity.KpiPoolItem;
import com.mihai.overview.entity.User;
import com.mihai.overview.exception.BadRequestException;
import com.mihai.overview.exception.ConflictException;
import com.mihai.overview.exception.ResourceNotFoundException;
import com.mihai.overview.repository.CriticalConditionPoolItemRepository;
import com.mihai.overview.repository.InteractionTypeRepository;
import com.mihai.overview.repository.KpiPoolItemRepository;
import com.mihai.overview.dto.request.CreateCriticalConditionPoolItemRequest;
import com.mihai.overview.dto.request.CreateInteractionTypeRequest;
import com.mihai.overview.dto.request.CreateKpiPoolItemRequest;
import com.mihai.overview.dto.response.CriticalConditionPoolItemResponse;
import com.mihai.overview.dto.response.InteractionTypeResponse;
import com.mihai.overview.dto.response.KpiPoolItemResponse;
import com.mihai.overview.security.FindAuthenticatedUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class ReviewConfigServiceImpl implements ReviewConfigService {

    private final InteractionTypeRepository interactionTypeRepository;
    private final KpiPoolItemRepository kpiPoolItemRepository;
    private final CriticalConditionPoolItemRepository criticalPoolRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;

    @Override
    @Transactional
    public InteractionTypeResponse createInteractionType(CreateInteractionTypeRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        if (request == null) {
            throw new BadRequestException("Request body is mandatory");
        }

        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new BadRequestException("InteractionType code is mandatory");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("InteractionType name is mandatory");
        }

        String normalizedCode = request.getCode().trim().toUpperCase();

        if (interactionTypeRepository.findByCode(normalizedCode).isPresent()) {
            throw new ConflictException("InteractionType already exists: " + normalizedCode);
        }

        InteractionType type = new InteractionType();
        type.setCode(normalizedCode);
        type.setName(request.getName().trim());
        type.setCreatedByUserId(currentUser.getId());
        type.setArchived(false);

        InteractionType saved = interactionTypeRepository.save(type);
        return new InteractionTypeResponse(saved.getId(), saved.getCode(), saved.getName());
    }

    @Override
    @Transactional
    public KpiPoolItemResponse createKpiPoolItem(String interactionTypeCode, CreateKpiPoolItemRequest request) {

        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        if (request == null) {
            throw new BadRequestException("Request body is mandatory");
        }

        if (interactionTypeCode == null || interactionTypeCode.trim().isEmpty()) {
            throw new BadRequestException("InteractionType code is mandatory");
        }

        String code = interactionTypeCode.trim().toUpperCase();

        InteractionType type = interactionTypeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("InteractionType not found: " + code));


        if (type.isArchived()) {
            throw new ConflictException("InteractionType is archived");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("KPI name is mandatory");
        }

        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new BadRequestException("KPI description is mandatory");
        }

        if (request.getDetails() == null || request.getDetails().trim().isEmpty()) {
            throw new BadRequestException("KPI details are mandatory");
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
                saved.getWeightPercent(),
                saved.isArchived()
        );
    }

    @Override
    @Transactional
    public CriticalConditionPoolItemResponse createCriticalConditionPoolItem(String interactionTypeCode, CreateCriticalConditionPoolItemRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        if (request == null) {
            throw new BadRequestException("Request body is mandatory");
        }

        if (interactionTypeCode == null || interactionTypeCode.trim().isEmpty()) {
            throw new BadRequestException("InteractionType code is mandatory");
        }

        String code = interactionTypeCode.trim().toUpperCase();

        InteractionType type = interactionTypeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("InteractionType not found: " + code));

        if (type.isArchived()) {
            throw new ConflictException("InteractionType is archived");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Critical name is mandatory");
        }

        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new BadRequestException("Critical description is mandatory");
        }

        CriticalConditionPoolItem c = new CriticalConditionPoolItem();
        c.setInteractionType(type);
        c.setName(request.getName().trim());
        c.setDescription(request.getDescription().trim());
        c.setCreatedByUserId(currentUser.getId());
        c.setArchived(false);

        CriticalConditionPoolItem saved = criticalPoolRepository.save(c);
        return new CriticalConditionPoolItemResponse(saved.getId(), type.getId(), saved.getName(), saved.getDescription(), saved.isArchived());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InteractionTypeResponse> listInteractionTypes() {
        return interactionTypeRepository.findAllByArchivedFalseOrderByCodeAsc().stream()
                .map(it -> new InteractionTypeResponse(it.getId(), it.getCode(), it.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KpiPoolItemResponse> listKpiPoolItems(String interactionTypeCode, boolean includeArchived) {

        if (interactionTypeCode == null || interactionTypeCode.trim().isEmpty()) {
            throw new BadRequestException("InteractionType code is mandatory");
        }
        String code = interactionTypeCode.trim().toUpperCase();

        InteractionType type = interactionTypeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("InteractionType not found: " + code));

        List<KpiPoolItem> items = includeArchived
                ? kpiPoolItemRepository.findAllByInteractionTypeIdOrderByNameAsc(type.getId())
                : kpiPoolItemRepository.findAllByInteractionTypeIdAndArchivedFalseOrderByNameAsc(type.getId());

        return items.stream()
                .map(k -> new KpiPoolItemResponse(
                        k.getId(),
                        type.getId(),
                        k.getName(),
                        k.getDescription(),
                        k.getDetails(),
                        k.getWeightPercent(),
                        k.isArchived()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriticalConditionPoolItemResponse> listCriticalConditionPoolItems(String interactionTypeCode, boolean includeArchived) {

        if (interactionTypeCode == null || interactionTypeCode.trim().isEmpty()) {
            throw new BadRequestException("InteractionType code is mandatory");
        }

        String code = interactionTypeCode.trim().toUpperCase();

        InteractionType type = interactionTypeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("InteractionType not found: " + code));

        List<CriticalConditionPoolItem> items = includeArchived
                ? criticalPoolRepository.findAllByInteractionTypeIdOrderByNameAsc(type.getId())
                : criticalPoolRepository.findAllByInteractionTypeIdAndArchivedFalseOrderByNameAsc(type.getId());

        return items.stream()
                .map(c -> new CriticalConditionPoolItemResponse(
                        c.getId(),
                        type.getId(),
                        c.getName(),
                        c.getDescription(),
                        c.isArchived()
                ))
                .toList();
    }


    @Override
    @Transactional
    public void archiveKpiPoolItem(String interactionTypeCode, Long kpiId) {

        if (interactionTypeCode == null || interactionTypeCode.trim().isEmpty()) {
            throw new BadRequestException("InteractionType code is mandatory");
        }

        if (kpiId == null || kpiId < 1) {
            throw new BadRequestException("kpiId must be positive");
        }

        String code = interactionTypeCode.trim().toUpperCase();

        InteractionType type = interactionTypeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("InteractionType not found: " + code));

        KpiPoolItem item = kpiPoolItemRepository.findByIdAndInteractionTypeId(kpiId, type.getId())
                .orElseThrow(() -> new ResourceNotFoundException("KPI not found for InteractionType: " + code));

        if (item.isArchived()) {
            throw new ConflictException("KPI is already archived");
        }

        item.setArchived(true);
    }

    @Override
    @Transactional
    public void unarchiveKpiPoolItem(String interactionTypeCode, Long kpiId) {

        if (interactionTypeCode == null || interactionTypeCode.trim().isEmpty()) {
            throw new BadRequestException("InteractionType code is mandatory");
        }

        if (kpiId == null || kpiId < 1) {
            throw new BadRequestException("kpiId must be positive");
        }

        String code = interactionTypeCode.trim().toUpperCase();

        InteractionType type = interactionTypeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("InteractionType not found: " + code));

        KpiPoolItem item = kpiPoolItemRepository.findByIdAndInteractionTypeId(kpiId, type.getId())
                .orElseThrow(() -> new ResourceNotFoundException("KPI not found for InteractionType: " + code));

        if (!item.isArchived()) {
            throw new ConflictException("KPI is already un-archived");
        }

        item.setArchived(false);
    }

    @Override
    @Transactional
    public void archiveCriticalPoolItem(String interactionTypeCode, Long criticalId) {

        if (interactionTypeCode == null || interactionTypeCode.trim().isEmpty()) {
            throw new BadRequestException("InteractionType code is mandatory");
        }

        if (criticalId == null || criticalId < 1) {
            throw new BadRequestException("criticalId must be positive");
        }

        String code = interactionTypeCode.trim().toUpperCase();

        InteractionType type = interactionTypeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("InteractionType not found: " + code));

        CriticalConditionPoolItem item = criticalPoolRepository.findByIdAndInteractionTypeId(criticalId, type.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Critical not found for InteractionType: " + code));

        if (item.isArchived()) {
            throw new ConflictException("Critical is already archived");
        }

        item.setArchived(true);
    }

    @Override
    @Transactional
    public void unarchiveCriticalPoolItem(String interactionTypeCode, Long criticalId) {

        if (interactionTypeCode == null || interactionTypeCode.trim().isEmpty()) {
            throw new BadRequestException("InteractionType code is mandatory");
        }

        if (criticalId == null || criticalId < 1) {
            throw new BadRequestException("criticalId must be positive");
        }

        String code = interactionTypeCode.trim().toUpperCase();

        InteractionType type = interactionTypeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("InteractionType not found: " + code));

        CriticalConditionPoolItem item = criticalPoolRepository.findByIdAndInteractionTypeId(criticalId, type.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Critical not found for InteractionType: " + code));

        if (!item.isArchived()) {
            throw new ConflictException("Critical is already un-archived");
        }

        item.setArchived(false);
    }
}
