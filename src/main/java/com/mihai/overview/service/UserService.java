package com.mihai.overview.service;


import com.mihai.overview.request.PasswordUpdateRequest;
import com.mihai.overview.response.UserListItemResponse;
import com.mihai.overview.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse getUserInfo();
    //void deleteUser();
    void updatePassword(PasswordUpdateRequest passwordUpdateRequest);
    List<UserListItemResponse> listUsers();
}
