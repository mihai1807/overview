package com.mihai.overview.service;

import com.mihai.overview.request.CreateSchemeRequest;
import com.mihai.overview.response.SchemeListItemStatusResponse;
import com.mihai.overview.response.SchemeResponse;

import java.util.List;

public interface SchemeAdminService {
    SchemeResponse createScheme(String code, CreateSchemeRequest request);

    List<SchemeListItemStatusResponse> listSchemesByInteractionType(String code, boolean includeArchived);

    void archiveScheme(Long schemeId);

    void unarchiveScheme(Long schemeId);
}