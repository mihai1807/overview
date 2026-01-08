package com.mihai.overview.response;

import com.mihai.overview.entity.Authority;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class UserResponse {

    private Long id;

    private String fullName;

    private String email;

    private List<Authority> authorities;


}
