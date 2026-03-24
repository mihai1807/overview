package com.mihai.overview.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateKpiPoolItemRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 500)
    private String description;

    @NotBlank
    private String details;

    @NotNull
    @Min(1)
    @Max(100)
    private int weightPercent;
}
