package com.mihai.overview.service;

import com.mihai.overview.request.AddKpiSchemeItemRequest;
import com.mihai.overview.request.CreateInteractionKpiRequest;
import com.mihai.overview.request.CreateKpiSchemeRequest;
import com.mihai.overview.request.CreateReviewTypeRequest;
import com.mihai.overview.request.UpdateKpiSchemeItemRequest;
import com.mihai.overview.request.UpdateKpiSchemeRequest;
import com.mihai.overview.response.InteractionKpiResponse;
import com.mihai.overview.response.KpiSchemeItemResponse;
import com.mihai.overview.response.KpiSchemeResponse;
import com.mihai.overview.response.ReviewTypeResponse;

public interface ReviewAdminService {

    ReviewTypeResponse createReviewType(CreateReviewTypeRequest request);

    InteractionKpiResponse createKpiUnderReviewType(Long reviewTypeId, CreateInteractionKpiRequest request);

    KpiSchemeResponse createSchemeUnderReviewType(Long reviewTypeId, CreateKpiSchemeRequest request);

    KpiSchemeItemResponse addItemToScheme(Long schemeId, AddKpiSchemeItemRequest request);

    ReviewTypeResponse activateSchemeForReviewType(Long reviewTypeId, Long schemeId);

    // NEW: version-safe update/delete
    KpiSchemeResponse updateScheme(Long schemeId, UpdateKpiSchemeRequest request);

    KpiSchemeResponse updateSchemeItemVersioned(Long schemeId, Long schemeItemId, UpdateKpiSchemeItemRequest request);

    KpiSchemeResponse deleteSchemeItemVersioned(Long schemeId, Long schemeItemId);

    void deleteScheme(Long schemeId);
}
