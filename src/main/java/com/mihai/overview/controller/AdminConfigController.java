package com.mihai.overview.controller;

import com.mihai.overview.request.CreateCriticalConditionPoolItemRequest;
import com.mihai.overview.request.CreateInteractionTypeRequest;
import com.mihai.overview.request.CreateKpiPoolItemRequest;
import com.mihai.overview.response.CriticalConditionPoolItemResponse;
import com.mihai.overview.response.InteractionTypeResponse;
import com.mihai.overview.response.KpiPoolItemResponse;
import com.mihai.overview.service.AdminConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@Tag(name = "Admin Config", description = "Create interaction types and manage KPI/Critical pools.")
@RequestMapping("/api/admin/config")
public class AdminConfigController {

    private final AdminConfigService adminConfigService;

    @PostMapping("/interaction-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InteractionTypeResponse> createInteractionType(@Valid @RequestBody CreateInteractionTypeRequest request) {
        return ResponseEntity.ok(adminConfigService.createInteractionType(request));
    }

    @PostMapping("/interaction-types/{code}/kpi-pool")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KpiPoolItemResponse> createKpi(@PathVariable String code,
                                                         @Valid @RequestBody CreateKpiPoolItemRequest request) {
        return ResponseEntity.ok(adminConfigService.createKpiPoolItem(code, request));
    }


    @PostMapping("/interaction-types/{code}/critical-pool")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CriticalConditionPoolItemResponse> createCritical(@PathVariable String code,
                                                                            @Valid @RequestBody CreateCriticalConditionPoolItemRequest request) {
        return ResponseEntity.ok(adminConfigService.createCriticalConditionPoolItem(code, request));
    }

}
