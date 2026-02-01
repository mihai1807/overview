package com.mihai.overview.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateReviewShellRequest {

    @NotNull
    private Long schemeId;

    @NotNull
    private Long reviewedUserId;

    @NotNull
    private Long ticketId;

    @NotNull
    private Long cid;

    @NotNull
    private LocalDateTime occurredAt;
}
