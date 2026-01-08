package com.mihai.overview.controller;

import com.mihai.overview.request.PasswordUpdateRequest;
import com.mihai.overview.response.UserResponse;
import com.mihai.overview.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RequestMapping ("/api/users")
@RestController
@Tag(name = "User REST API Endpoints", description = "Operations related to info about current user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/info")
    private UserResponse getUserInfo() {
        return userService.getUserInfo();
    }

    @DeleteMapping
    public void deleteUser() {
        userService.deleteUser();
    }

    @PutMapping("/password")
    public void passwordUpdate(@Valid @RequestBody PasswordUpdateRequest passwordUpdateRequest)
            throws Exception {
        userService.updatePassword(passwordUpdateRequest);
    }
}
