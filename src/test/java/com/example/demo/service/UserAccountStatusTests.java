package com.example.demo.service;

import com.example.demo.dto.request.User.ChangeEmailRequest;
import com.example.demo.dto.request.User.UpdateUserRequest;
import com.example.demo.dto.request.VerifyChangeEmailRequest;
import com.example.demo.entity.OTP;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.respository.UserRepository;
import com.example.demo.respository.VerifyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserAccountStatusTests {
    private UserRepository users;
    private VerifyRepository otps;
    private UserService service;
    private User user;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        otps = mock(VerifyRepository.class);
        service = new UserService();
        ReflectionTestUtils.setField(service, "userRepository", users);
        ReflectionTestUtils.setField(service, "verifyRepository", otps);
        user = User.builder().Id(10L).username("alice").email("alice@example.test")
                .fullname("Alice").status(UserStatus.ACTIVE).build();
        when(users.findByUsernameForUpdate("alice")).thenReturn(Optional.of(user));
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"PENDING", "LOCKED", "DISABLED"})
    void profileAndEmailWritesRecheckStatusAfterAuthentication(UserStatus status) {
        user.setStatus(status);
        // An expired timer must not let these endpoints reactivate an account.
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(1);
        user.setLockUntill(deadline);
        Runnable[] actions = {
                () -> service.updateUser(profile(), "alice"),
                () -> service.changeEmail(new ChangeEmailRequest("new@example.test"), "alice"),
                () -> service.verifyChangeEmail(new VerifyChangeEmailRequest("123456"), "alice")
        };
        for (Runnable action : actions) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class, action::run);
            assertEquals(status == UserStatus.LOCKED ? HttpStatus.LOCKED : HttpStatus.UNAUTHORIZED,
                    error.getStatusCode());
        }
        assertEquals(status, user.getStatus());
        assertEquals(deadline, user.getLockUntill());
        assertEquals("Alice", user.getFullname());
        assertEquals("alice@example.test", user.getEmail());
        verify(users, never()).save(any());
        verifyNoInteractions(otps);
    }

    @Test
    void activeUserCanUpdateProfileUsingTheLockedRecord() {
        service.updateUser(profile(), "alice");

        assertEquals("Updated Alice", user.getFullname());
        assertEquals(LocalDate.of(2000, 1, 1), user.getDoB());
        assertEquals("0901234567", user.getPhone());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(users).findByUsernameForUpdate("alice");
        verify(users, never()).findByUsername(any());
        verify(users).save(user);
    }

    @Test
    void activeUserCanVerifyEmailWithoutChangingAccountStatus() {
        OTP otp = OTP.builder().userId(10L).purpose("CHANGE_EMAIL").otpCode("123456")
                .target("new@example.test").expriedAt(LocalDateTime.now().plusMinutes(5)).build();
        when(otps.findByUserIdAndPurpose(10L, "CHANGE_EMAIL")).thenReturn(Optional.of(otp));

        service.verifyChangeEmail(new VerifyChangeEmailRequest("123456"), "alice");

        assertEquals("new@example.test", user.getEmail());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(users).findByUsernameForUpdate("alice");
        verify(users).save(user);
        verify(otps).delete(otp);
    }

    private UpdateUserRequest profile() {
        return new UpdateUserRequest("Updated Alice", LocalDate.of(2000, 1, 1), "0901234567");
    }
}
