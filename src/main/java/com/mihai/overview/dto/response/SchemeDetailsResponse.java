package com.mihai.overview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class SchemeDetailsResponse {
    private Long schemeId;
    private String schemeName;
    private String interactionTypeCode;

    private List<KpiInScheme> kpis;
    private List<CriticalInScheme> criticals;

    @AllArgsConstructor
    @Getter
    public static class KpiInScheme {
        private Long kpiId;
        private String name;
        private String description;
        private String details;
        private int weightPercent;
        private int orderIndex;
        private boolean required;
    }

    @AllArgsConstructor
    @Getter
    public static class CriticalInScheme {
        private Long criticalId;
        private String name;
        private String description;
        private int orderIndex;
    }
}
