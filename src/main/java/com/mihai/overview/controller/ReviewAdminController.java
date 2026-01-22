package com.mihai.overview.controller;

import com.mihai.overview.request.AddKpiSchemeItemRequest;
import com.mihai.overview.request.CreateInteractionKpiRequest;
import com.mihai.overview.request.CreateKpiSchemeRequest;
import com.mihai.overview.request.CreateReviewTypeRequest;
import com.mihai.overview.response.InteractionKpiResponse;
import com.mihai.overview.response.KpiSchemeItemResponse;
import com.mihai.overview.response.KpiSchemeResponse;
import com.mihai.overview.response.ReviewTypeResponse;
import com.mihai.overview.service.ReviewAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.mihai.overview.request.UpdateKpiSchemeItemRequest;
import com.mihai.overview.request.UpdateKpiSchemeRequest;

@Tag(name = "Admin Review Config API", description = "Admin operations for review types, KPIs, and KPI schemes")
@AllArgsConstructor
@RestController
@RequestMapping("/api/admin/review-config")
public class ReviewAdminController {

    private final ReviewAdminService reviewAdminService;

    @Operation(summary = "Create review type")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/review-types")
    public ReviewTypeResponse createReviewType(@Valid @RequestBody CreateReviewTypeRequest request) {
        return reviewAdminService.createReviewType(request);
    }

    @Operation(summary = "Create KPI under a review type")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/review-types/{reviewTypeId}/kpis")
    public InteractionKpiResponse createKpi(@PathVariable Long reviewTypeId,
                                            @Valid @RequestBody CreateInteractionKpiRequest request) {
        return reviewAdminService.createKpiUnderReviewType(reviewTypeId, request);
    }

    @Operation(summary = "Create scheme under a review type")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/review-types/{reviewTypeId}/schemes")
    public KpiSchemeResponse createScheme(@PathVariable Long reviewTypeId,
                                          @Valid @RequestBody CreateKpiSchemeRequest request) {
        return reviewAdminService.createSchemeUnderReviewType(reviewTypeId, request);
    }

    @Operation(summary = "Add KPI item to a scheme")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/schemes/{schemeId}/items")
    public KpiSchemeItemResponse addSchemeItem(@PathVariable Long schemeId,
                                               @Valid @RequestBody AddKpiSchemeItemRequest request) {
        return reviewAdminService.addItemToScheme(schemeId, request);
    }

    @Operation(summary = "Activate a scheme for a review type (weights must total 100)")
    @PutMapping("/review-types/{reviewTypeId}/activate-scheme/{schemeId}")
    public ReviewTypeResponse activateScheme(@PathVariable Long reviewTypeId,
                                             @PathVariable Long schemeId) {
        return reviewAdminService.activateSchemeForReviewType(reviewTypeId, schemeId);
    }

    @PutMapping("/schemes/{schemeId}")
    public KpiSchemeResponse updateScheme(@PathVariable Long schemeId,
                                          @Valid @RequestBody UpdateKpiSchemeRequest request) {
        return reviewAdminService.updateScheme(schemeId, request);
    }

    @PutMapping("/schemes/{schemeId}/items/{schemeItemId}")
    public KpiSchemeResponse updateSchemeItemVersioned(@PathVariable Long schemeId,
                                                       @PathVariable Long schemeItemId,
                                                       @Valid @RequestBody UpdateKpiSchemeItemRequest request) {
        return reviewAdminService.updateSchemeItemVersioned(schemeId, schemeItemId, request);
    }

    @DeleteMapping("/schemes/{schemeId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteScheme(@PathVariable Long schemeId) {
        reviewAdminService.deleteScheme(schemeId);
    }

    @DeleteMapping("/schemes/{schemeId}/items/{schemeItemId}")
    public KpiSchemeResponse deleteSchemeItemVersioned(@PathVariable Long schemeId,
                                                       @PathVariable Long schemeItemId) {
        return reviewAdminService.deleteSchemeItemVersioned(schemeId, schemeItemId);
    }

}

