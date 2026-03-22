package com.mihai.overview.service;

import com.mihai.overview.exception.ConflictException;
import com.mihai.overview.exception.ForbiddenException;
import com.mihai.overview.exception.ResourceNotFoundException;
import com.mihai.overview.security.AppRole;
import com.mihai.overview.entity.Authority;
import com.mihai.overview.entity.User;
import com.mihai.overview.repository.UserRepository;
import com.mihai.overview.dto.request.AdminUpdateUserRequest;
import com.mihai.overview.dto.request.PromoteUserRequest;
import com.mihai.overview.security.FindAuthenticatedUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;

    @Override
    @Transactional
    public User promoteUser(Long userId, PromoteUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        AppRole targetRole = AppRole.fromAuthorityOrName(request.getRole());
        String targetAuthority = targetRole.asAuthority();

        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> AppRole.ADMIN.asAuthority().equals(a.getAuthority()));

        List<Authority> updated = new ArrayList<>();
        updated.add(new Authority(targetAuthority));
        if (isAdmin && !AppRole.ADMIN.asAuthority().equals(targetAuthority)) {
            updated.add(new Authority(AppRole.ADMIN.asAuthority()));
        }

        user.setAuthorities(updated);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User grantAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        boolean alreadyAdmin = user.getAuthorities().stream()
                .anyMatch(a -> AppRole.ADMIN.asAuthority().equals(a.getAuthority()));

        if (!alreadyAdmin) {
            List<Authority> updated = new ArrayList<>(authorityEntities(user));
            updated.add(new Authority(AppRole.ADMIN.asAuthority()));
            user.setAuthorities(updated);
            user = userRepository.save(user);
        }

        return user;
    }

    @Override
    @Transactional
    public User revokeAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> AppRole.ADMIN.asAuthority().equals(a.getAuthority()));

        if (!isAdmin) {
            throw new ConflictException("User does not have admin role.");
        }

        long adminCount = userRepository.countUsersWithAuthority(AppRole.ADMIN.asAuthority());
        if (adminCount <= 1) {
            throw new ConflictException("Cannot revoke admin: at least one admin must remain");
        }

        List<Authority> updated = user.getAuthorities().stream()
                .filter(a -> !AppRole.ADMIN.asAuthority().equals(a.getAuthority()))
                .map(a -> (Authority) a)
                .toList();

        if (updated.isEmpty()) {
            throw new ConflictException("Cannot revoke admin: user must have at least one role");
        }

        user.setAuthorities(updated);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void disableUser(Long userId) {
        User actingAdmin = findAuthenticatedUser.getAuthenticatedUser();

        if (actingAdmin.getId().equals(userId)) {
            throw new ForbiddenException("You cannot disable your own account.");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        boolean targetIsAdmin = target.getAuthorities().stream()
                .anyMatch(a -> AppRole.ADMIN.asAuthority().equals(a.getAuthority()));

        if (targetIsAdmin) {
            long adminCount = userRepository.countUsersWithAuthority(AppRole.ADMIN.asAuthority());
            if (adminCount <= 1) {
                throw new ConflictException("Cannot disable the last admin.");
            }
        }

        if (!target.isEnabled()) {
            throw new ConflictException("User is already disabled.");
        }

        target.setEnabled(false);
        target.setDisabledAt(Instant.now());
        userRepository.save(target);
    }

    @Override
    @Transactional
    public void reinstateUser(Long userId) {
        User actingAdmin = findAuthenticatedUser.getAuthenticatedUser();

        if (actingAdmin.getId().equals(userId)) {
            throw new ForbiddenException("You cannot reinstate your own account.");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (target.isEnabled()) {
            throw new ConflictException("User is already enabled.");
        }

        target.setEnabled(true);
        target.setDisabledAt(null);
        userRepository.save(target);
    }

    private List<Authority> authorityEntities(User user) {
        return user.getAuthorities().stream()
                .map(a -> (Authority) a)
                .toList();
    }

    @Override
    @Transactional
    public User updateUserDetails(Long userId, AdminUpdateUserRequest request) {
        User actingAdmin = findAuthenticatedUser.getAuthenticatedUser();

        if (actingAdmin.getId().equals(userId)) {
            throw new ForbiddenException("You cannot edit your own account.");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (!target.isEnabled()) {
            throw new ConflictException("Cannot edit a disabled user");
        }

        String newEmail = request.getEmail().trim();

        if (!newEmail.equalsIgnoreCase(target.getEmail())) {
            Optional<User> existing = userRepository.findByEmail(newEmail);
            if (existing.isPresent()) {
                throw new ConflictException("Email already in use: " + newEmail);
            }
            target.setEmail(newEmail);
        }

        target.setFirstName(request.getFirstName().trim());
        target.setLastName(request.getLastName().trim());

        return userRepository.save(target);
    }
}
