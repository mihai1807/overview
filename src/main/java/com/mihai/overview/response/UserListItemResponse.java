package com.mihai.overview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class UserListItemResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;

    // store role strings to avoid leaking embeddable/entity shape
    private List<String> authorities;

    private boolean enabled;
    private Instant disabledAt;

    private Instant createdAt;
    private Instant updatedAt;
}