package com.mihai.overview.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateKpiSchemeRequest {

    @NotBlank
    @Size(max = 100)
    private String name; // e.g. chat1
}
