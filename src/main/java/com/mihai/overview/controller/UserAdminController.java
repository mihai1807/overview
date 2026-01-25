package com.mihai.overview.controller;

import com.mihai.overview.entity.User;
import com.mihai.overview.request.PromoteUserRequest;
import com.mihai.overview.service.UserAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@Tag(name = "User Admin REST API Endpoints", description = "Operations related to updating roles and admin access.")
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @PutMapping("/{userId}/promote")
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

    @PutMapping("/{userId}/grant-admin")
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

    @PutMapping("/{userId}/revoke-admin")
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

}
