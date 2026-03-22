package com.mihai.overview.service;

import com.mihai.overview.entity.User;
import com.mihai.overview.dto.request.PromoteUserRequest;
import com.mihai.overview.dto.request.AdminUpdateUserRequest;

public interface UserAdminService {
    User promoteUser(Long userId, PromoteUserRequest request);
    User grantAdmin(Long userId);
    User revokeAdmin(Long userId);

    // ✅ NEW: soft delete
    void disableUser(Long userId);

    void reinstateUser(Long userId);

    User updateUserDetails(Long userId, AdminUpdateUserRequest request);
}
