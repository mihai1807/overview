package com.mihai.overview.service;

import com.mihai.overview.response.ActiveSchemeResponse;

public interface ReviewConfigService {
    ActiveSchemeResponse getActiveSchemeForReviewTypeCode(String reviewTypeCode);
}
