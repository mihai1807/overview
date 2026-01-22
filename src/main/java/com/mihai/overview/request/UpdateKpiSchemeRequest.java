package com.mihai.overview.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateKpiSchemeRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
}
