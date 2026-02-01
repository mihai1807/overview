package com.mihai.overview.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateReviewCriticalHitsRequest {

    // null or empty => means "none triggered"
    private List<Long> triggeredCriticalIds;
}
