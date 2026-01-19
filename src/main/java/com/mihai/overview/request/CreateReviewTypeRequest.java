package com.mihai.overview.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReviewTypeRequest {

    @NotBlank
    @Size(max = 50)
    private String code; // e.g. CHAT

    @NotBlank
    @Size(max = 100)
    private String name; // e.g. Chat Review
}
