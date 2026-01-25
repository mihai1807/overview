package com.mihai.overview.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CreateReviewRequest {

    @NotNull
    private Long schemeId;

    @NotNull
    private Long reviewedUserId;

    @NotNull
    private Long reviewerId;

    private Long ticketId;
    private Long cid;

    @NotNull
    private LocalDateTime occurredAt;

    @NotNull
    private List<KpiScoreInput> kpiScores;

    @NotNull
    private List<CriticalHitInput> criticalHits;

    @Getter
    @Setter
    public static class KpiScoreInput {
        @NotNull
        private Long kpiId;

        @Min(0) @Max(100)
        private int score;
    }

    @Getter
    @Setter
    public static class CriticalHitInput {
        @NotNull
        private Long criticalId;

        private boolean triggered;
    }
}
