package com.mihai.overview.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateUserRequest {

    @NotBlank(message = "firstName is mandatory")
    @Size(max = 50, message = "firstName must be at most 50 characters")
    private String firstName;

    @NotBlank(message = "lastName is mandatory")
    @Size(max = 50, message = "lastName must be at most 50 characters")
    private String lastName;

    @NotBlank(message = "email is mandatory")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "email must be at most 100 characters")
    private String email;
}