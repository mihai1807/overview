package com.mihai.overview.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInteractionTypeRequest {

    @NotBlank
    @Size(max = 50)
    private String code; // CHAT, EMAIL, etc.

    @NotBlank
    @Size(max = 100)
    private String name; // display label
}
