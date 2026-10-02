package com.example.demo.service;

import com.example.demo.dto.request.User.AdminUpdateUserRequest;
import com.example.demo.dto.response.AdminUserResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.respository.RefreshTokenRepository;
import com.example.demo.respository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AdminUserService(UserRepository userRepository,
                            RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> searchUsers(String username, String email,
                                               String role, String status) {
        String normalizedStatus = normalize(status);
        if (normalizedStatus != null) {
            try {
                normalizedStatus = UserStatus.valueOf(normalizedStatus.toUpperCase(Locale.ROOT)).name();
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Status must be PENDING, ACTIVE, LOCKED or DISABLED");
            }
        }
        return userRepository.searchUsers(normalize(username), normalize(email),
                        normalize(role), normalizedStatus)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long id) {
        return toResponse(userRepository.findById(id).orElseThrow(this::notFound));
    }

    public AdminUserResponse updateUser(Long id, AdminUpdateUserRequest request) {
        User user = findForUpdate(id);
        if (request.getUsername() != null && !Objects.equals(user.getUsername(), request.getUsername())) {
            throw conflict("Username is immutable because it identifies the account and login sessions");
        }
        userRepository.findByEmail(request.getEmail())
                .filter(existing -> !Objects.equals(existing.getId(), id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
                });

        boolean identityChanged = !Objects.equals(user.getEmail(), request.getEmail());
        user.setEmail(request.getEmail());
        user.setFullname(request.getFullname());
        user.setDoB(request.getDoB());
        user.setPhone(request.getPhone());
        return save(user, identityChanged);
    }

    public AdminUserResponse lockUser(Long id) {
        User user = findForUpdate(id);
        if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.LOCKED) {
            throw conflict("Only an active account can be locked");
        }
        user.setStatus(UserStatus.LOCKED);
        // A manual lock has no expiry, including when replacing a temporary login lock.
        user.setLockUntill(null);
        return save(user, true);
    }

    public AdminUserResponse unlockUser(Long id) {
        User user = findForUpdate(id);
        if (user.getStatus() != UserStatus.LOCKED && user.getStatus() != UserStatus.ACTIVE) {
            throw conflict("Only a locked account can be unlocked; use activate for a disabled or pending account");
        }
        user.setStatus(UserStatus.ACTIVE);
        resetLoginFailures(user);
        return save(user, false);
    }

    public AdminUserResponse disableUser(Long id) {
        User user = findForUpdate(id);
        user.setStatus(UserStatus.DISABLED);
        resetLoginFailures(user);
        return save(user, true);
    }

    public AdminUserResponse activateUser(Long id) {
        User user = findForUpdate(id);
        if (user.getStatus() == UserStatus.LOCKED) {
            throw conflict("Account is locked; use unlock to restore access");
        }
        user.setStatus(UserStatus.ACTIVE);
        resetLoginFailures(user);
        return save(user, false);
    }

    private User findForUpdate(Long id) {
        return userRepository.findByIdForUpdate(id).orElseThrow(this::notFound);
    }

    private AdminUserResponse save(User user, boolean revokeSessions) {
        userRepository.save(user);
        // Materialize lazy roles before a bulk token update can clear the persistence context.
        AdminUserResponse response = toResponse(user);
        if (revokeSessions) {
            refreshTokenRepository.revokeByUserId(user.getId());
        }
        return response;
    }

    private void resetLoginFailures(User user) {
        user.setFailedLoginAttemps(0);
        user.setLockUntill(null);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getFullname(), user.getDoB(), user.getPhone(), user.getStatus(),
                user.getRoles().stream().map(Role::getName).map(Enum::name).collect(Collectors.toSet()),
                user.getLockUntill());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    private ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }
}
