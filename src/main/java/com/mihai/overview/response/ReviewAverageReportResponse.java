package com.mihai.overview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class ReviewAverageReportResponse {

    private long reviewCount;
    private long sumScore;
    private double averageScore; // rounded to 2 decimals

    // structured warnings (non-fatal)
    private List<ReportWarningResponse> warnings;
}