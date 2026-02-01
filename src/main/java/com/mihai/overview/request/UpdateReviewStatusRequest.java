package com.mihai.overview.request;

import com.mihai.overview.entity.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReviewStatusRequest {

    @NotNull
    private ReviewStatus status;
}
