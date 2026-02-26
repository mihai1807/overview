package com.mihai.overview.repository.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ReviewAverageAgg {
    private long reviewCount;
    private long sumScore;
}