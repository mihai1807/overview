package com.mihai.overview.controller;

import com.mihai.overview.entity.User;
import com.mihai.overview.request.AdminUpdateUserRequest;
import com.mihai.overview.request.PromoteUserRequest;
import com.mihai.overview.response.AdminUpdateUserResponse;
import com.mihai.overview.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@Tag(
        name = "User Admin REST API Endpoints",
        description = "Admin-only operations for managing user roles, enabling/disabling accounts, and updating user profile details."
)
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @Operation(
            summary = "Promote/demote user role",
            description = "Sets the user's primary non-admin role to the provided role (e.g., AGENT, SHIFT_MANAGER, TEAM_MANAGER, QUALITY_ANALYST). " +
                    "If the user already has ROLE_ADMIN, it is preserved unless you explicitly revoke admin via the revoke-admin endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid role"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized (ADMIN only)"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> promote(@PathVariable Long userId, @Valid @RequestBody PromoteUserRequest request) {
        User updated = userAdminService.promoteUser(userId, request);

        return ResponseEntity.ok(
                java.util.Map.of(
                        "id", updated.getId(),
                        "email", updated.getEmail(),
                        "authorities", updated.getAuthorities().stream().map(a -> a.getAuthority()).toList()
                )
        );
    }

    @Operation(
            summary = "Grant admin access",
            description = "Adds ROLE_ADMIN to the user's authorities. Idempotent (no change if already admin)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin granted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized (ADMIN only)"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}/grant-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> grantAdmin(@PathVariable Long userId) {
        User updated = userAdminService.grantAdmin(userId);
        return ResponseEntity.ok(
                java.util.Map.of(
                        "id", updated.getId(),
                        "email", updated.getEmail(),
                        "authorities", updated.getAuthorities().stream().map(a -> a.getAuthority()).toList()
                )
        );
    }

    @Operation(
            summary = "Revoke admin access",
            description = "Removes ROLE_ADMIN from the user's authorities. Prevents revoking the last remaining admin."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin revoked successfully"),
            @ApiResponse(responseCode = "400", description = "User would be left with no roles"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized (ADMIN only) or cannot revoke last admin"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}/revoke-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> revokeAdmin(@PathVariable Long userId) {
        User updated = userAdminService.revokeAdmin(userId);
        return ResponseEntity.ok(
                java.util.Map.of(
                        "id", updated.getId(),
                        "email", updated.getEmail(),
                        "authorities", updated.getAuthorities().stream().map(a -> a.getAuthority()).toList()
                )
        );
    }

    @Operation(
            summary = "Disable a user (soft delete)",
            description = "Disables the user account (enabled=false) and sets disabledAt. " +
                    "Blocks disabling yourself and blocks disabling the last remaining admin."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User disabled successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized (ADMIN only) or cannot disable self/last admin"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> disableUser(@PathVariable Long userId) {
        userAdminService.disableUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Reinstate a user",
            description = "Re-enables a previously disabled user account (enabled=true) and clears disabledAt. Blocks reinstating yourself."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User reinstated successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized (ADMIN only) or cannot reinstate self"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}/reinstate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reinstateUser(@PathVariable Long userId) {
        userAdminService.reinstateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Update user profile (admin-only)",
            description = "Updates firstName, lastName, and email for the specified user. " +
                    "Rules: cannot edit yourself; cannot edit disabled users; email must be unique. " +
                    "Changing email forces re-login because JWT subject is the email."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input / email already in use / user disabled"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized (ADMIN only) or cannot edit self"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserDetails(@PathVariable Long userId, @Valid @RequestBody AdminUpdateUserRequest request) {
        User updated = userAdminService.updateUserDetails(userId, request);

        return ResponseEntity.ok(new AdminUpdateUserResponse(
                updated.getFirstName(),
                updated.getLastName(),
                updated.getEmail(),
                updated.getUpdatedAt()
        ));
    }
}