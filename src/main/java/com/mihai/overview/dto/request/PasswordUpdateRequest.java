package com.mihai.overview.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class PasswordUpdateRequest {
    @NotEmpty(message = "Old password is mandatory.")
    @Size(min=5, max=30, message = "The old password must be at least 5 characters long.")
    private String oldPassword;

    @NotEmpty(message = "New password is mandatory.")
    @Size(min=5, max=30, message = "The new password must be at least 5 characters long.")
    private String newPassword;

    @NotEmpty(message = "New password is mandatory.")
    @Size(min=5, max=30, message = "The new password must be at least 5 characters long.")
    private String newPassword2;
}
