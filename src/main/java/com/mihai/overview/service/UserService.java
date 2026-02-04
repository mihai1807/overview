package com.mihai.overview.service;


import com.mihai.overview.request.PasswordUpdateRequest;
import com.mihai.overview.response.UserResponse;

public interface UserService {
    UserResponse getUserInfo();
    //void deleteUser();
    void updatePassword(PasswordUpdateRequest passwordUpdateRequest);
}
