package com.mihai.overview.response;

import com.mihai.overview.entity.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class ReviewDetailsResponse {

    private Long id;
    private Long schemeId;
    private String interactionTypeCode;
    private String periodKey;
    private Integer totalScore; // null until KPI scores filled
    private ReviewStatus status;

    private List<KpiLine> kpis;
    private List<CriticalLine> criticals;

    @AllArgsConstructor
    @Getter
    public static class KpiLine {
        private Long kpiId;
        private String name;
        private int weightPercent;
        private boolean required;
        private Integer score; // null until filled
    }

    @AllArgsConstructor
    @Getter
    public static class CriticalLine {
        private Long criticalId;
        private String name;
        private boolean triggered;
    }
}
