package com.mihai.overview.controller;

import com.mihai.overview.response.ActiveSchemeResponse;
import com.mihai.overview.service.ReviewConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Review Config REST API", description = "Operations for review KPI configuration")
@AllArgsConstructor
@RestController
@RequestMapping("/api/review-config")
public class ReviewConfigController {

    private final ReviewConfigService reviewConfigService;

    @Operation(summary = "Get active scheme by review type code", description = "Returns the active KPI scheme and its items for the given review type code")
    @GetMapping("/active-scheme/{reviewTypeCode}")
    public ResponseEntity<ActiveSchemeResponse> getActiveScheme(@PathVariable String reviewTypeCode) {
        return ResponseEntity.ok(reviewConfigService.getActiveSchemeForReviewTypeCode(reviewTypeCode));
    }
}
