package com.mihai.overview.dto.response;

import com.mihai.overview.entity.Authority;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private List<Authority> authorities;

    // ✅ NEW
    private boolean enabled;
    private Instant disabledAt;
}