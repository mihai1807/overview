package com.mihai.overview.service;

import com.mihai.overview.dto.request.CreateSchemeRequest;
import com.mihai.overview.dto.response.SchemeDetailsResponse;
import com.mihai.overview.dto.response.SchemeListItemStatusResponse;
import com.mihai.overview.dto.response.SchemeResponse;

import java.util.List;

public interface SchemeAdminService {

    SchemeResponse createScheme(String code, CreateSchemeRequest request);

    List<SchemeListItemStatusResponse> listSchemesByInteractionType(String code, boolean includeArchived);

    // ✅ NEW (moved from ConfigQueryService)
    SchemeDetailsResponse getSchemeDetails(Long schemeId, boolean includeArchived);

    void archiveScheme(Long schemeId);

    void unarchiveScheme(Long schemeId);
}