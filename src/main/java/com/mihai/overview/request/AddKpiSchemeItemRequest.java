package com.mihai.overview.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddKpiSchemeItemRequest {

    @NotNull
    private Long kpiId;

    @Min(0)
    private int minScore;

    @Min(1)
    private int maxScore;

    @Min(1)
    @Max(100)
    private int weightPercent;

    @Min(0)
    private int orderIndex;

    private boolean required = true;
}
