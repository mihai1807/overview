package com.mihai.overview.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateReviewKpiScoresRequest {

    @NotNull
    @Valid
    private List<KpiScoreInput> kpiScores;

    @Getter
    @Setter
    public static class KpiScoreInput {
        @NotNull
        private Long kpiId;

        @Min(0) @Max(100)
        private int score;
    }
}
