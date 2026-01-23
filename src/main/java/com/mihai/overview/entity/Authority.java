package com.mihai.overview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

@Embeddable
public class Authority implements GrantedAuthority {

    @Column(name = "authority", nullable = false, length = 50)
    private String authority;

    public Authority() {}

    public Authority(String authority) {
        this.authority = authority;
    }

    @Override
    public @Nullable String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }
}
