package com.mihai.overview.service;

import com.mihai.overview.dto.response.SchemeDetailsResponse;
import com.mihai.overview.dto.response.SchemeListItemResponse;

import java.util.List;

public interface ConfigQueryService {
    List<SchemeListItemResponse> listSchemesForInteractionTypeCode(String interactionTypeCode);
    SchemeDetailsResponse getSchemeDetails(Long schemeId);
}
