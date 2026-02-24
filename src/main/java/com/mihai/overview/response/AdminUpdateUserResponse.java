package com.mihai.overview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@Getter
@Setter
public class AdminUpdateUserResponse {

    private String firstName;
    private String lastName;
    private String email;
    private Instant updatedAt;
}