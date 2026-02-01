package com.mihai.overview.controller;

import com.mihai.overview.request.CreateSchemeRequest;
import com.mihai.overview.response.SchemeResponse;
import com.mihai.overview.service.SchemeAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@Tag(name = "Scheme Admin", description = "Create schemes by selecting KPIs and critical conditions from pools.")
@RequestMapping("/api/admin/schemes")
public class SchemeAdminController {

    private final SchemeAdminService schemeAdminService;

    @PostMapping("/interaction-types/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchemeResponse> createScheme(
            @PathVariable String code,
            @Valid @RequestBody CreateSchemeRequest request
    ) {
        return ResponseEntity.ok(schemeAdminService.createScheme(code, request));
    }
}
