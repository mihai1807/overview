package com.mihai.overview.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReviewKpiScoreRequest {

    @NotNull
    private Long schemeItemId;

    @Min(0)
    private int score;
}
