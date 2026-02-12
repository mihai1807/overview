package com.mihai.overview.controller;

import com.mihai.overview.request.CreateCriticalConditionPoolItemRequest;
import com.mihai.overview.request.CreateInteractionTypeRequest;
import com.mihai.overview.request.CreateKpiPoolItemRequest;
import com.mihai.overview.response.CriticalConditionPoolItemResponse;
import com.mihai.overview.response.InteractionTypeResponse;
import com.mihai.overview.response.KpiPoolItemResponse;
import com.mihai.overview.service.ReviewConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@Tag(name = "Review Config", description = "Configure review interaction types and manage KPI/Critical pools.")
@RequestMapping("/api/admin/review-config")

public class ReviewConfigController {

    private final ReviewConfigService reviewConfigService;

    @PostMapping("/interaction-types")
    @PreAuthorize("hasAnyRole('ADMIN','QUALITY_ANALYST','TEAM_MANAGER')")
    public ResponseEntity<InteractionTypeResponse> createInteractionType(@Valid @RequestBody CreateInteractionTypeRequest request) {
        return ResponseEntity.ok(reviewConfigService.createInteractionType(request));
    }

    @PostMapping("/interaction-types/{code}/kpi-pool")
    @PreAuthorize("hasAnyRole('ADMIN','QUALITY_ANALYST','TEAM_MANAGER')")
    public ResponseEntity<KpiPoolItemResponse> createKpi(
            @PathVariable String code,
            @Valid @RequestBody CreateKpiPoolItemRequest request
    ) {
        return ResponseEntity.ok(reviewConfigService.createKpiPoolItem(code, request));
    }

    @PostMapping("/interaction-types/{code}/critical-pool")
    @PreAuthorize("hasAnyRole('ADMIN','QUALITY_ANALYST','TEAM_MANAGER')")
    public ResponseEntity<CriticalConditionPoolItemResponse> createCritical(
            @PathVariable String code,
            @Valid @RequestBody CreateCriticalConditionPoolItemRequest request
    ) {
        return ResponseEntity.ok(reviewConfigService.createCriticalConditionPoolItem(code, request));
    }

    @GetMapping("/interaction-types")
    @PreAuthorize("hasAnyRole('ADMIN','QUALITY_ANALYST','TEAM_MANAGER')")
    public ResponseEntity<List<InteractionTypeResponse>> listInteractionTypes() {
        return ResponseEntity.ok(reviewConfigService.listInteractionTypes());
    }

    @GetMapping("/interaction-types/{code}/kpi-pool")
    @PreAuthorize("hasAnyRole('ADMIN','QUALITY_ANALYST','TEAM_MANAGER')")
    public ResponseEntity<List<KpiPoolItemResponse>> listKpis(@PathVariable String code) {
        return ResponseEntity.ok(reviewConfigService.listKpiPoolItems(code));
    }

    @GetMapping("/interaction-types/{code}/critical-pool")
    @PreAuthorize("hasAnyRole('ADMIN','QUALITY_ANALYST','TEAM_MANAGER')")
    public ResponseEntity<List<CriticalConditionPoolItemResponse>> listCriticals(@PathVariable String code) {
        return ResponseEntity.ok(reviewConfigService.listCriticalConditionPoolItems(code));
    }
}
