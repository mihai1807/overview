package com.mihai.overview.service;

import com.mihai.overview.config.AppRole;
import com.mihai.overview.entity.Authority;
import com.mihai.overview.entity.User;
import com.mihai.overview.repository.UserRepository;
import com.mihai.overview.request.PromoteUserRequest;
import com.mihai.overview.util.FindAuthenticatedUser;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;

    @Override
    @Transactional
    public User promoteUser(Long userId, PromoteUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        AppRole targetRole = AppRole.fromAuthorityOrName(request.getRole());
        String targetAuthority = targetRole.asAuthority();

        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        List<Authority> updated = new ArrayList<>();
        updated.add(new Authority(targetAuthority));
        if (isAdmin && !"ROLE_ADMIN".equals(targetAuthority)) {
            updated.add(new Authority("ROLE_ADMIN"));
        }

        user.setAuthorities(updated);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User grantAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        boolean alreadyAdmin = user.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!alreadyAdmin) {
            List<Authority> updated = new ArrayList<>(authorityEntities(user));
            updated.add(new Authority("ROLE_ADMIN"));
            user.setAuthorities(updated);
            user = userRepository.save(user);
        }

        return user;
    }

    @Override
    @Transactional
    public User revokeAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin) {
            return user;
        }

        long adminCount = userRepository.countUsersWithAuthority("ROLE_ADMIN");
        if (adminCount <= 1) {
            throw new IllegalStateException("Cannot revoke admin: at least one admin must remain");
        }

        List<Authority> updated = user.getAuthorities().stream()
                .filter(a -> !"ROLE_ADMIN".equals(a.getAuthority()))
                .map(a -> (Authority) a)
                .toList();

        if (updated.isEmpty()) {
            throw new IllegalArgumentException("User must have at least one role");
        }

        user.setAuthorities(updated);
        return userRepository.save(user);
    }

    // ✅ NEW: soft delete / disable user (admin-only at controller level)
    @Override
    @Transactional
    public void disableUser(Long userId) {
        User actingAdmin = findAuthenticatedUser.getAuthenticatedUser();

        if (actingAdmin.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot disable your own account");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));

        boolean targetIsAdmin = target.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (targetIsAdmin) {
            long adminCount = userRepository.countUsersWithAuthority("ROLE_ADMIN");
            if (adminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot disable the last admin");
            }
        }

        // idempotent: if already disabled, keep as-is
        if (!target.isEnabled()) {
            return;
        }

        target.setEnabled(false);
        target.setDisabledAt(Instant.now());
        userRepository.save(target);
    }

    private List<Authority> authorityEntities(User user) {
        return user.getAuthorities().stream()
                .map(a -> (Authority) a)
                .toList();
    }
}
