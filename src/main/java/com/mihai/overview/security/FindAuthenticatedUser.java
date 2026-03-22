package com.mihai.overview.security;

import com.mihai.overview.entity.User;

public interface FindAuthenticatedUser {
    User getAuthenticatedUser();
}
