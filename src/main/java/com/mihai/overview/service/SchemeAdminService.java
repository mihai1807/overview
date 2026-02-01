package com.mihai.overview.service;

import com.mihai.overview.request.CreateSchemeRequest;
import com.mihai.overview.response.SchemeResponse;

public interface SchemeAdminService {
    SchemeResponse createScheme(String code, CreateSchemeRequest request);
}