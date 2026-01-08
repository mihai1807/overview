package com.mihai.overview.util;

import com.mihai.overview.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class FindAuthenticatedUserImpl implements FindAuthenticatedUser{

    @Override
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null || !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            throw new AccessDeniedException("Authentication Required.");
        }
        return (User) authentication.getPrincipal();
    }
}
