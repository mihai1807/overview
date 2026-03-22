package com.mihai.overview.security;

import com.mihai.overview.exception.BadRequestException;

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
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Role is mandatory");
        }

        String normalized = value.trim();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }

        try {
            return AppRole.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role: " + value);
        }
    }
}
