package com.mihai.overview.service;


import com.mihai.overview.dto.request.PasswordUpdateRequest;
import com.mihai.overview.dto.response.UserListItemResponse;
import com.mihai.overview.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse getUserInfo();
    //void deleteUser();
    void updatePassword(PasswordUpdateRequest passwordUpdateRequest);
    List<UserListItemResponse> listUsers();
}
