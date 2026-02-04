package com.mihai.overview.controller;

import com.mihai.overview.request.PasswordUpdateRequest;
import com.mihai.overview.response.UserResponse;
import com.mihai.overview.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    private UserResponse getUserInfo() {
        return userService.getUserInfo();
    }

/*    @Operation (summary = "Delete user", description = "Delete current user account")
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping
    public void deleteUser() {
        userService.deleteUser();
    }*/

    @Operation (summary = "Password update", description = "Change user password after verification")
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/password")
    public void passwordUpdate(@Valid @RequestBody PasswordUpdateRequest passwordUpdateRequest)
            throws Exception {
        userService.updatePassword(passwordUpdateRequest);
    }
}
