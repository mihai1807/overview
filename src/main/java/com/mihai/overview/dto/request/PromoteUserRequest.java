package com.mihai.overview.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromoteUserRequest {

    @NotBlank(message = "role is mandatory")
    private String role; // e.g. "SHIFT_MANAGER" or "ROLE_SHIFT_MANAGER"
}