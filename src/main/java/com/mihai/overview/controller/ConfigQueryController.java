package com.mihai.overview.controller;

import com.mihai.overview.response.SchemeDetailsResponse;
import com.mihai.overview.response.SchemeListItemResponse;
import com.mihai.overview.service.ConfigQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@Tag(name = "Config Queries", description = "Dropdown/query endpoints for schemes and scheme details.")
@RequestMapping("/api/config")
public class ConfigQueryController {

    private final ConfigQueryService configQueryService;

    @GetMapping("/interaction-types/{code}/schemes")
    public ResponseEntity<List<SchemeListItemResponse>> listSchemes(@PathVariable String code) {
        return ResponseEntity.ok(configQueryService.listSchemesForInteractionTypeCode(code));
    }

    @GetMapping("/schemes/{schemeId}")
    public ResponseEntity<SchemeDetailsResponse> getSchemeDetails(@PathVariable Long schemeId) {
        return ResponseEntity.ok(configQueryService.getSchemeDetails(schemeId));
    }
}
