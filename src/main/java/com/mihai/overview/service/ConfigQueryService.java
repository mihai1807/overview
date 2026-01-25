package com.mihai.overview.service;

import com.mihai.overview.response.SchemeDetailsResponse;
import com.mihai.overview.response.SchemeListItemResponse;

import java.util.List;

public interface ConfigQueryService {
    List<SchemeListItemResponse> listSchemesForInteractionTypeCode(String interactionTypeCode);
    SchemeDetailsResponse getSchemeDetails(Long schemeId);
}
