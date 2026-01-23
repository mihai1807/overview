package com.mihai.overview.service;

import com.mihai.overview.entity.User;
import com.mihai.overview.request.PromoteUserRequest;

public interface UserAdminService {
    User promoteUser(Long userId, PromoteUserRequest request);
    User grantAdmin(Long userId);
    User revokeAdmin(Long userId);
}
