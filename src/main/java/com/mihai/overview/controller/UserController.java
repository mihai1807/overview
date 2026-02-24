package com.mihai.overview.controller;

import com.mihai.overview.request.PasswordUpdateRequest;
import com.mihai.overview.response.UserListItemResponse;
import com.mihai.overview.response.UserResponse;
import com.mihai.overview.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping ("/api/users")
@RestController
@Tag(name = "User REST API Endpoints", description = "Operations related to info about current user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation (summary = "User Information", description = "Get current user info")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/info")
    public UserResponse getUserInfo() {
        return userService.getUserInfo();
    }


    @Operation(summary = "Password update", description = "Change current user's password after verifying the old password")
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/password")
    public void passwordUpdate(@Valid @RequestBody PasswordUpdateRequest passwordUpdateRequest) {
        userService.updatePassword(passwordUpdateRequest);
    }

    @Operation(summary = "List users", description = "List all users including roles and enabled/disabled status (non-agent roles only)")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','QUALITY_ANALYST','TEAM_MANAGER','SHIFT_MANAGER')")
    public List<UserListItemResponse> listUsers() {
        return userService.listUsers();
    }
}
