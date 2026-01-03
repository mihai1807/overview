package com.mihai.overview.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class AuthenticationRequest {

    @NotEmpty(message = "Email is mandatory")
    @Email (message = "Invalid email format")
    private String email;

    @NotEmpty(message = "Password is mandatory")
    @Size(min=5, max=30, message = "Password must be at least 5 characters long.")
    private String password;

    public @NotEmpty(message = "Email is mandatory") @Email(message = "Invalid email format") String getEmail() {
        return email;
    }

    public @NotEmpty(message = "Password is mandatory") @Size(min = 5, max = 30, message = "Password must be at least 5 characters long.") String getPassword() {
        return password;
    }
}
