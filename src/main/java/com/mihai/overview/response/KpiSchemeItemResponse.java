package com.mihai.overview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class KpiSchemeItemResponse {
    private Long id;
    private Long schemeId;
    private Long kpiId;
    private String kpiName;
    private int minScore;
    private int maxScore;
    private int weightPercent;
    private int orderIndex;
    private boolean required;
}
