package com.mihai.overview.config;

import java.util.Arrays;

public enum AppRole {
    AGENT,
    SHIFT_MANAGER,
    TEAM_MANAGER,
    QUALITY_ANALYST,
    ADMIN;

    public String asAuthority() {
        return "ROLE_" + name();
    }

    public static AppRole fromAuthorityOrName(String value) {
        if (value.startsWith("ROLE_")) {
            value = value.substring("ROLE_".length());
        }
        return AppRole.valueOf(value);
    }
}
