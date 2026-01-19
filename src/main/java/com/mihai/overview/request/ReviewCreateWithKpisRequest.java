package com.mihai.overview.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReviewCreateWithKpisRequest {

    @NotNull
    private Long ticketId;

    @NotBlank
    private String reviewTypeCode; // e.g. CHAT

    @NotNull
    private LocalDate interactionDate;

    @NotNull
    private LocalTime interactionTime;

    @NotNull
    private Long cid;

    @NotNull
    private Long reviewedUserId;

    @Valid
    @NotNull
    private List<ReviewKpiScoreRequest> kpis;
}
