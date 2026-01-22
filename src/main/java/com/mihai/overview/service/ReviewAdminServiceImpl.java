package com.mihai.overview.service;

import com.mihai.overview.entity.*;
import com.mihai.overview.repository.*;
import com.mihai.overview.request.*;
import com.mihai.overview.response.InteractionKpiResponse;
import com.mihai.overview.response.KpiSchemeItemResponse;
import com.mihai.overview.response.KpiSchemeResponse;
import com.mihai.overview.response.ReviewTypeResponse;
import com.mihai.overview.util.FindAuthenticatedUser;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
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
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final ReviewRepository reviewRepository;
    private final ReviewKpiScoreRepository reviewKpiScoreRepository;

    @Override
    @Transactional
    public ReviewTypeResponse createReviewType(CreateReviewTypeRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        ReviewType type = new ReviewType();
        type.setCode(request.getCode().trim().toUpperCase());
        type.setName(request.getName().trim());

        // Ownership
        type.setCreatedByUserId(currentUser.getId());

        ReviewType saved = reviewTypeRepository.save(type);

        return new ReviewTypeResponse(
                saved.getId(),
                saved.getCode(),
                saved.getName(),
                saved.getActiveScheme() == null ? null : saved.getActiveScheme().getId()
        );
    }

    @Override
    @Transactional
    public InteractionKpiResponse createKpiUnderReviewType(Long reviewTypeId, CreateInteractionKpiRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        ReviewType type = reviewTypeRepository.findById(reviewTypeId)
                .orElseThrow(() -> new IllegalArgumentException("ReviewType not found: " + reviewTypeId));

        InteractionKpi kpi = new InteractionKpi();
        kpi.setReviewType(type);
        kpi.setName(request.getName().trim());
        kpi.setDescription(request.getDescription().trim());
        kpi.setActive(true);

        // Archive/ownership (assumes you added these fields)
        kpi.setArchived(false);
        kpi.setCreatedByUserId(currentUser.getId());

        InteractionKpi saved = interactionKpiRepository.save(kpi);

        return new InteractionKpiResponse(saved.getId(), type.getId(), saved.getName(), saved.getDescription(), saved.isActive());
    }

    @Override
    @Transactional
    public KpiSchemeResponse createSchemeUnderReviewType(Long reviewTypeId, CreateKpiSchemeRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        ReviewType type = reviewTypeRepository.findById(reviewTypeId)
                .orElseThrow(() -> new IllegalArgumentException("ReviewType not found: " + reviewTypeId));

        KpiScheme scheme = new KpiScheme();
        scheme.setReviewType(type);
        scheme.setName(request.getName().trim());
        scheme.setActive(false);

        // Archive/ownership (assumes you added these fields)
        scheme.setArchived(false);
        scheme.setCreatedByUserId(currentUser.getId());

        KpiScheme saved = kpiSchemeRepository.save(scheme);

        return new KpiSchemeResponse(saved.getId(), type.getId(), saved.getName(), saved.isActive());
    }

    @Override
    @Transactional
    public KpiSchemeItemResponse addItemToScheme(Long schemeId, AddKpiSchemeItemRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        KpiScheme scheme = kpiSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        InteractionKpi kpi = interactionKpiRepository.findById(request.getKpiId())
                .orElseThrow(() -> new IllegalArgumentException("KPI not found: " + request.getKpiId()));

        if (scheme.isArchived()) {
            throw new IllegalArgumentException("Cannot modify an archived scheme");
        }

        // Only owner or admin can modify scheme
        requireOwnerOrAdmin(scheme.getCreatedByUserId(), currentUser);

        // Enforce: KPI must belong to the same ReviewType as the scheme
        if (!kpi.getReviewType().getId().equals(scheme.getReviewType().getId())) {
            throw new IllegalArgumentException("KPI does not belong to the scheme's ReviewType");
        }

        // Optional: block using archived KPI
        if (kpi.isArchived()) {
            throw new IllegalArgumentException("Cannot add an archived KPI to a scheme");
        }

        if (request.getMaxScore() <= request.getMinScore()) {
            throw new IllegalArgumentException("maxScore must be greater than minScore");
        }

        // Enforce: scheme weights cannot exceed 100 while building it (ignore archived items)
        List<KpiSchemeItem> existing = kpiSchemeItemRepository.findByScheme_Id(schemeId)
                .stream()
                .filter(i -> !i.isArchived())
                .toList();

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

        // Archive/ownership (assumes you added these fields)
        item.setArchived(false);
        item.setCreatedByUserId(currentUser.getId());

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
            throw new IllegalArgumentException("This KPI is already added to the scheme");
        }
    }

    @Override
    @Transactional
    public ReviewTypeResponse activateSchemeForReviewType(Long reviewTypeId, Long schemeId) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        ReviewType type = reviewTypeRepository.findById(reviewTypeId)
                .orElseThrow(() -> new IllegalArgumentException("ReviewType not found: " + reviewTypeId));

        KpiScheme scheme = kpiSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        // Only owner of ReviewType or admin can switch active scheme
        requireOwnerOrAdmin(type.getCreatedByUserId(), currentUser);

        if (scheme.isArchived()) {
            throw new IllegalArgumentException("Cannot activate an archived scheme");
        }

        if (!scheme.getReviewType().getId().equals(type.getId())) {
            throw new IllegalArgumentException("Scheme does not belong to the given ReviewType");
        }

        List<KpiSchemeItem> items = kpiSchemeItemRepository.findByScheme_Id(schemeId)
                .stream()
                .filter(i -> !i.isArchived())
                .toList();

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

        return new ReviewTypeResponse(
                savedType.getId(),
                savedType.getCode(),
                savedType.getName(),
                savedType.getActiveScheme() == null ? null : savedType.getActiveScheme().getId()
        );
    }

    // ---------------------------
    // NEW METHODS (version-safe)
    // ---------------------------

    @Override
    @Transactional
    public KpiSchemeResponse updateScheme(Long schemeId, UpdateKpiSchemeRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        KpiScheme scheme = kpiSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        requireOwnerOrAdmin(scheme.getCreatedByUserId(), currentUser);

        if (scheme.isArchived()) {
            throw new IllegalArgumentException("Cannot update an archived scheme");
        }

        boolean used = reviewRepository.existsByKpiScheme_Id(schemeId);

        if (used) {
            // clone and rename clone; old scheme untouched
            KpiScheme cloned = cloneSchemeWithItems(
                    scheme,
                    currentUser.getId(),
                    request.getName().trim()
            );
            return new KpiSchemeResponse(cloned.getId(), cloned.getReviewType().getId(), cloned.getName(), cloned.isActive());
        }

        scheme.setName(request.getName().trim());
        KpiScheme saved = kpiSchemeRepository.save(scheme);
        return new KpiSchemeResponse(saved.getId(), saved.getReviewType().getId(), saved.getName(), saved.isActive());
    }

    @Override
    @Transactional
    public KpiSchemeResponse updateSchemeItemVersioned(Long schemeId, Long schemeItemId, UpdateKpiSchemeItemRequest request) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        KpiScheme scheme = kpiSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        requireOwnerOrAdmin(scheme.getCreatedByUserId(), currentUser);

        if (scheme.isArchived()) {
            throw new IllegalArgumentException("Cannot modify an archived scheme");
        }

        KpiSchemeItem item = kpiSchemeItemRepository.findById(schemeItemId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme item not found: " + schemeItemId));

        if (!item.getScheme().getId().equals(scheme.getId())) {
            throw new IllegalArgumentException("Scheme item does not belong to scheme");
        }

        if (item.isArchived()) {
            throw new IllegalArgumentException("Cannot update an archived scheme item");
        }

        if (request.getMaxScore() <= request.getMinScore()) {
            throw new IllegalArgumentException("maxScore must be greater than minScore");
        }

        boolean used = reviewRepository.existsByKpiScheme_Id(schemeId);

        if (used) {
            String newName = scheme.getName().trim() + "_v2";
            KpiScheme cloned = cloneSchemeWithItems(scheme, currentUser.getId(), newName);

            Long kpiId = item.getKpi().getId();
            KpiSchemeItem clonedItem = kpiSchemeItemRepository.findByScheme_Id(cloned.getId()).stream()
                    .filter(x -> !x.isArchived() && x.getKpi().getId().equals(kpiId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Cloned item not found"));

            applyItemUpdate(cloned.getId(), clonedItem, request);

            return new KpiSchemeResponse(cloned.getId(), cloned.getReviewType().getId(), cloned.getName(), cloned.isActive());
        }

        applyItemUpdate(schemeId, item, request);
        return new KpiSchemeResponse(scheme.getId(), scheme.getReviewType().getId(), scheme.getName(), scheme.isActive());
    }

    @Override
    @Transactional
    public KpiSchemeResponse deleteSchemeItemVersioned(Long schemeId, Long schemeItemId) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        KpiScheme scheme = kpiSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        requireOwnerOrAdmin(scheme.getCreatedByUserId(), currentUser);

        if (scheme.isArchived()) {
            throw new IllegalArgumentException("Cannot modify an archived scheme");
        }

        KpiSchemeItem item = kpiSchemeItemRepository.findById(schemeItemId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme item not found: " + schemeItemId));

        if (!item.getScheme().getId().equals(scheme.getId())) {
            throw new IllegalArgumentException("Scheme item does not belong to scheme");
        }

        if (item.isArchived()) {
            throw new IllegalArgumentException("Scheme item already archived");
        }

        boolean used = reviewRepository.existsByKpiScheme_Id(schemeId);

        if (used) {
            String newName = scheme.getName().trim() + "_v2";
            KpiScheme cloned = cloneSchemeWithItems(scheme, currentUser.getId(), newName);

            Long kpiId = item.getKpi().getId();
            KpiSchemeItem clonedItem = kpiSchemeItemRepository.findByScheme_Id(cloned.getId()).stream()
                    .filter(x -> !x.isArchived() && x.getKpi().getId().equals(kpiId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Cloned item not found"));

            // Archive the item in the clone (do not delete)
            clonedItem.setArchived(true);
            kpiSchemeItemRepository.save(clonedItem);

            return new KpiSchemeResponse(cloned.getId(), cloned.getReviewType().getId(), cloned.getName(), cloned.isActive());
        }

        // If any review KPI score references this item, do NOT hard delete
        if (reviewKpiScoreRepository.existsBySchemeItem_Id(schemeItemId)) {
            throw new IllegalArgumentException("Cannot delete scheme item: it is referenced by existing review KPI scores");
        }

        kpiSchemeItemRepository.delete(item);
        return new KpiSchemeResponse(scheme.getId(), scheme.getReviewType().getId(), scheme.getName(), scheme.isActive());
    }

    @Override
    @Transactional
    public void deleteScheme(Long schemeId) {
        User currentUser = findAuthenticatedUser.getAuthenticatedUser();

        KpiScheme scheme = kpiSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        requireOwnerOrAdmin(scheme.getCreatedByUserId(), currentUser);

        boolean used = reviewRepository.existsByKpiScheme_Id(schemeId);

        if (used) {
            // archive only (never delete historical config used by reviews)
            scheme.setActive(false);
            scheme.setArchived(true);
            kpiSchemeRepository.save(scheme);

            ReviewType type = scheme.getReviewType();
            if (type.getActiveScheme() != null && type.getActiveScheme().getId().equals(schemeId)) {
                type.setActiveScheme(null);
                reviewTypeRepository.save(type);
            }
            return;
        }

        // Unused: safe to hard delete
        kpiSchemeRepository.delete(scheme);
    }

    // ---------------------------
    // Helpers
    // ---------------------------

    private void requireOwnerOrAdmin(Long createdByUserId, User currentUser) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> {
                    String auth = a.getAuthority();
                    return "ROLE_ADMIN".equals(auth) || "ADMIN".equals(auth);
                });

        // legacy rows (null creator): admin only
        if (createdByUserId == null) {
            if (!isAdmin) {
                throw new AccessDeniedException("Forbidden");
            }
            return;
        }

        if (createdByUserId.equals(currentUser.getId())) {
            return;
        }

        if (!isAdmin) {
            throw new AccessDeniedException("Forbidden");
        }
    }

    private KpiScheme cloneSchemeWithItems(KpiScheme original, Long creatorId, String newName) {
        KpiScheme clone = new KpiScheme();
        clone.setReviewType(original.getReviewType());
        clone.setName(newName);
        clone.setActive(false);
        clone.setArchived(false);
        clone.setCreatedByUserId(creatorId);

        KpiScheme savedClone = kpiSchemeRepository.save(clone);

        List<KpiSchemeItem> items = kpiSchemeItemRepository.findByScheme_Id(original.getId());
        for (KpiSchemeItem it : items) {
            if (it.isArchived()) continue;

            KpiSchemeItem copy = new KpiSchemeItem();
            copy.setScheme(savedClone);
            copy.setKpi(it.getKpi());
            copy.setMinScore(it.getMinScore());
            copy.setMaxScore(it.getMaxScore());
            copy.setWeightPercent(it.getWeightPercent());
            copy.setOrderIndex(it.getOrderIndex());
            copy.setRequired(it.isRequired());
            copy.setArchived(false);
            copy.setCreatedByUserId(creatorId);

            kpiSchemeItemRepository.save(copy);
        }

        return savedClone;
    }

    private void applyItemUpdate(Long schemeId, KpiSchemeItem item, UpdateKpiSchemeItemRequest request) {
        // Ensure weights won't exceed 100 in THIS scheme (ignore archived items)
        List<KpiSchemeItem> all = kpiSchemeItemRepository.findByScheme_Id(schemeId).stream()
                .filter(x -> !x.isArchived())
                .toList();

        int otherWeights = all.stream()
                .filter(x -> !x.getId().equals(item.getId()))
                .mapToInt(KpiSchemeItem::getWeightPercent)
                .sum();

        if (otherWeights + request.getWeightPercent() > 100) {
            throw new IllegalArgumentException("Total scheme weight cannot exceed 100%. Current(other): " + otherWeights);
        }

        item.setMinScore(request.getMinScore());
        item.setMaxScore(request.getMaxScore());
        item.setWeightPercent(request.getWeightPercent());
        item.setOrderIndex(request.getOrderIndex());
        item.setRequired(request.isRequired());

        kpiSchemeItemRepository.save(item);
    }
}
