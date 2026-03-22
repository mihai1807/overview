package com.mihai.overview.service;


import com.mihai.overview.entity.Authority;
import com.mihai.overview.entity.User;
import com.mihai.overview.exception.BadRequestException;
import com.mihai.overview.repository.UserRepository;
import com.mihai.overview.dto.request.PasswordUpdateRequest;
import com.mihai.overview.dto.response.UserListItemResponse;
import com.mihai.overview.dto.response.UserResponse;
import com.mihai.overview.security.AppRole;
import com.mihai.overview.security.FindAuthenticatedUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private  final UserRepository userRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;
    private final PasswordEncoder passwordEncoder;


    public UserServiceImpl(UserRepository userRepository, FindAuthenticatedUser findAuthenticatedUser, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserInfo() {
        User user = findAuthenticatedUser.getAuthenticatedUser();
        return new UserResponse(
                user.getId(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getAuthorities().stream().map(auth -> (Authority) auth).toList(),
                user.isEnabled(),
                user.getDisabledAt()
        );
    }



    @Transactional
    @Override
    public void updatePassword(PasswordUpdateRequest passwordUpdateRequest) {
        User user = findAuthenticatedUser.getAuthenticatedUser();

        if (!isOldPasswordCorrect(passwordUpdateRequest.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (!isNewPasswordConfirmed(passwordUpdateRequest.getNewPassword(), passwordUpdateRequest.getNewPassword2())) {
            throw new BadRequestException("New passwords do not match.");
        }
        if (isNewPasswordTheSameAsOldPassword(passwordUpdateRequest.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password cannot be the same as the old password.");
        }

        user.setPassword(passwordEncoder.encode(passwordUpdateRequest.getNewPassword()));

        userRepository.save(user);

    }

    @Override
    @Transactional(readOnly = true)
    public List<UserListItemResponse> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(u -> new UserListItemResponse(
                        u.getId(),
                        u.getFirstName(),
                        u.getLastName(),
                        u.getFirstName() + " " + u.getLastName(),
                        u.getEmail(),
                        u.getAuthorities().stream().map(a -> a.getAuthority()).toList(),
                        u.isEnabled(),
                        u.getDisabledAt(),
                        u.getCreatedAt(),
                        u.getUpdatedAt()
                ))
                .toList();
    }

    private boolean isOldPasswordCorrect (String oldPass, String currPass) {
        return passwordEncoder.matches(oldPass, currPass);
    }

    private boolean isNewPasswordConfirmed (String newPass, String newPassConf) {
        return newPass.equals(newPassConf);
    }

    private boolean isNewPasswordTheSameAsOldPassword (String newPass, String oldPass) {
        return passwordEncoder.matches(newPass, oldPass);
    }



    private boolean isLastAdmin(User user){
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(authority -> AppRole.ADMIN.asAuthority().equals(authority.getAuthority()));
        if (isAdmin) {
            long adminCount = userRepository.countAdminUsers();
            return adminCount<=1;
        }
        return false;
    }
}
