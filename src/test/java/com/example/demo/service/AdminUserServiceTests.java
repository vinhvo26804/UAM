package com.example.demo.service;

import com.example.demo.dto.request.User.AdminUpdateUserRequest;
import com.example.demo.dto.response.AdminUserResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.respository.RefreshTokenRepository;
import com.example.demo.respository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AdminUserServiceTests {
    private static final Long USER_ID = 12L;
    private static final LocalDate BIRTH_DATE = LocalDate.of(1998, 3, 14);
    private static final LocalDateTime LOCK_EXPIRY = LocalDateTime.of(2030, 1, 1, 12, 0);

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private AdminUserService service;
    private User user;
    private Set<Role> roles;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        service = new AdminUserService(userRepository, refreshTokenRepository);
        Role role = new Role();
        role.setName(Role.RoleType.USER);
        roles = Set.of(role);
        user = User.builder()
                .Id(USER_ID)
                .username("alice")
                .password("original-password-hash")
                .email("alice@example.test")
                .fullname("Alice Original")
                .doB(BIRTH_DATE)
                .phone("0901234567")
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .build();
        user.setFailedLoginAttemps(4);
        user.setLockUntill(LOCK_EXPIRY);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    }

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            "PENDING, lock, , false",
            "ACTIVE, lock, LOCKED, true",
            "LOCKED, lock, LOCKED, true",
            "DISABLED, lock, , false",
            "PENDING, unlock, , false",
            "ACTIVE, unlock, ACTIVE, false",
            "LOCKED, unlock, ACTIVE, false",
            "DISABLED, unlock, , false",
            "PENDING, disable, DISABLED, true",
            "ACTIVE, disable, DISABLED, true",
            "LOCKED, disable, DISABLED, true",
            "DISABLED, disable, DISABLED, true",
            "PENDING, activate, ACTIVE, false",
            "ACTIVE, activate, ACTIVE, false",
            "LOCKED, activate, , false",
            "DISABLED, activate, ACTIVE, false"
    })
    void statusTransitionsPreserveAccountsAndEnforceDistinctRecoveryActions(
            UserStatus initial, String action, UserStatus expected, boolean revokeSessions) {
        user.setStatus(initial);

        if (expected == null) {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> invoke(action));
            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            assertEquals(initial, user.getStatus());
            assertEquals(4, user.getFailedLoginAttemps());
            assertEquals(LOCK_EXPIRY, user.getLockUntill());
        } else {
            AdminUserResponse response = invoke(action);
            assertEquals(expected, user.getStatus());
            assertEquals(expected, response.status());
            assertEquals(USER_ID, response.id());
            assertEquals(Set.of("USER"), response.roles());
            // Re-locking a temporary lock makes it permanent; recovery and disabling reset failures.
            assertNull(user.getLockUntill());
            assertNull(response.lockUntill());
            if (!action.equals("lock")) {
                assertEquals(0, user.getFailedLoginAttemps());
            }
            verify(userRepository).save(user);
        }

        assertOriginalProfile();
        if (revokeSessions) {
            verify(refreshTokenRepository).revokeByUserId(USER_ID);
            verifyNoMoreInteractions(refreshTokenRepository);
        } else {
            verifyNoInteractions(refreshTokenRepository);
        }
        verify(userRepository).findByIdForUpdate(USER_ID);
        // Also prevents physical deletion or role removal on disable, including repeat requests.
        verifyNoMoreInteractions(userRepository);
    }

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    void updatingProfilePreservesAccountStatusAndRevokesSessionsWhenEmailChanges(UserStatus status) {
        user.setStatus(status);
        AdminUpdateUserRequest request = updateRequest();

        AdminUserResponse response = service.updateUser(USER_ID, request);

        assertEquals(request.getEmail(), user.getEmail());
        assertEquals(request.getFullname(), user.getFullname());
        assertEquals(request.getDoB(), user.getDoB());
        assertEquals(request.getPhone(), user.getPhone());
        assertEquals(new AdminUserResponse(USER_ID, "alice", request.getEmail(), request.getFullname(),
                request.getDoB(), request.getPhone(), status, Set.of("USER"), LOCK_EXPIRY), response);
        assertEquals(status, user.getStatus());
        assertEquals(4, user.getFailedLoginAttemps());
        assertEquals(LOCK_EXPIRY, user.getLockUntill());
        assertIdentityAndRoles();
        verify(userRepository).findByIdForUpdate(USER_ID);
        verify(userRepository).findByEmail(request.getEmail());
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeByUserId(USER_ID);
        verifyNoMoreInteractions(userRepository, refreshTokenRepository);
    }

    @Test
    void updatingOtherProfileFieldsAllowsOwnEmailAndUnchangedUsernameWithoutRevokingSessions() {
        AdminUpdateUserRequest request = updateRequest();
        request.setUsername(user.getUsername());
        request.setEmail(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        AdminUserResponse response = service.updateUser(USER_ID, request);

        assertEquals(request.getFullname(), response.fullname());
        assertEquals(request.getDoB(), response.doB());
        assertEquals(request.getPhone(), response.phone());
        assertIdentityAndRoles();
        verify(userRepository).save(user);
        verifyNoInteractions(refreshTokenRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "username"})
    void identityConflictsRejectTheWholeProfileUpdate(String conflict) {
        AdminUpdateUserRequest request = updateRequest();
        if (conflict.equals("email")) {
            User otherUser = User.builder().Id(99L).email(request.getEmail()).build();
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(otherUser));
        } else {
            request.setUsername("another-username");
        }

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateUser(USER_ID, request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertOriginalProfile();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(4, user.getFailedLoginAttemps());
        assertEquals(LOCK_EXPIRY, user.getLockUntill());
        verify(userRepository).findByIdForUpdate(USER_ID);
        if (conflict.equals("email")) {
            verify(userRepository).findByEmail(request.getEmail());
        }
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(refreshTokenRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"get", "update", "lock", "unlock", "disable", "activate"})
    void missingUsersReturnNotFoundWithoutAnyWrite(String action) {
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> invoke(action));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        if (action.equals("get")) {
            verify(userRepository).findById(USER_ID);
        } else {
            verify(userRepository).findByIdForUpdate(USER_ID);
        }
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void searchNormalizesFiltersAndIncludesDisabledUsers() {
        user.setStatus(UserStatus.DISABLED);
        when(userRepository.searchUsers("alice", null, "USER", "DISABLED")).thenReturn(List.of(user));

        List<AdminUserResponse> results = service.searchUsers(" alice ", "  ", " USER ", " disabled ");

        assertEquals(1, results.size());
        assertEquals(USER_ID, results.get(0).id());
        assertEquals(UserStatus.DISABLED, results.get(0).status());
        verify(userRepository).searchUsers("alice", null, "USER", "DISABLED");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void invalidSearchStatusReturnsBadRequestBeforeQueryingUsers() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.searchUsers(null, null, null, " unknown "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(userRepository, refreshTokenRepository);
    }

    private AdminUserResponse invoke(String action) {
        return switch (action) {
            case "get" -> service.getUser(USER_ID);
            case "update" -> service.updateUser(USER_ID, updateRequest());
            case "lock" -> service.lockUser(USER_ID);
            case "unlock" -> service.unlockUser(USER_ID);
            case "disable" -> service.disableUser(USER_ID);
            case "activate" -> service.activateUser(USER_ID);
            default -> throw new IllegalArgumentException("Unknown test action: " + action);
        };
    }

    private AdminUpdateUserRequest updateRequest() {
        return new AdminUpdateUserRequest(null, "new-alice@example.test", "Alice Updated",
                LocalDate.of(1999, 4, 15), "0912345678");
    }

    private void assertOriginalProfile() {
        assertIdentityAndRoles();
        assertEquals("alice@example.test", user.getEmail());
        assertEquals("Alice Original", user.getFullname());
        assertEquals(BIRTH_DATE, user.getDoB());
        assertEquals("0901234567", user.getPhone());
    }

    private void assertIdentityAndRoles() {
        assertEquals(USER_ID, user.getId());
        assertEquals("alice", user.getUsername());
        assertEquals("original-password-hash", user.getPassword());
        assertSame(roles, user.getRoles());
        assertEquals(Set.of(Role.RoleType.USER),
                user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));
    }
}
