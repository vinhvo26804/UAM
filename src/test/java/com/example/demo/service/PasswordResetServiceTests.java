package com.example.demo.service;

import com.example.demo.dto.request.ForgotPasswordRequest;
import com.example.demo.dto.request.ResetPasswordRequest;
import com.example.demo.entity.PasswordResetOtp;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.respository.PasswordResetOtpRepository;
import com.example.demo.respository.RefreshTokenRepository;
import com.example.demo.respository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PasswordResetServiceTests {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final AtomicReference<PasswordResetOtp> storedOtp = new AtomicReference<>();
    private final List<String> deliveredCodes = new ArrayList<>();
    private PasswordResetService service;
    private UserRepository userRepository;
    private PasswordResetOtpRepository otpRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordResetOtpSender sender;
    private User alice;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        otpRepository = mock(PasswordResetOtpRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        sender = mock(PasswordResetOtpSender.class);
        service = new PasswordResetService(userRepository, otpRepository,
                refreshTokenRepository, passwordEncoder, sender);

        alice = User.builder()
                .Id(11L)
                .username("alice")
                .email("alice@example.test")
                .password(passwordEncoder.encode("old-password"))
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findByUsernameForUpdate("alice")).thenReturn(Optional.of(alice));
        when(otpRepository.findByUserId(11L))
                .thenAnswer(invocation -> Optional.ofNullable(storedOtp.get()));
        when(otpRepository.save(any(PasswordResetOtp.class))).thenAnswer(invocation -> {
            PasswordResetOtp value = invocation.getArgument(0);
            value.setId(101L);
            storedOtp.set(value);
            return value;
        });
        doAnswer(invocation -> {
            deliveredCodes.add(invocation.getArgument(1));
            return null;
        }).when(sender).send(eq("alice@example.test"), anyString());
    }

    @Test
    void requestSendsSixDigitOtpToRegisteredEmailAndStoresOnlyItsHash() {
        LocalDateTime startedAt = LocalDateTime.now();

        service.requestOtp(forgot("alice"));

        assertEquals(1, deliveredCodes.size());
        String code = latestCode();
        assertTrue(code.matches("[0-9]{6}"));
        PasswordResetOtp stored = storedOtp.get();
        assertNotNull(stored);
        assertEquals(alice.getId(), stored.getUserId());
        assertNotEquals(code, stored.getOtpHash());
        assertTrue(passwordEncoder.matches(code, stored.getOtpHash()));
        assertFalse(stored.isConsumed());
        assertEquals(0, stored.getFailedAttempts());
        assertFalse(stored.getRequestedAt().isBefore(startedAt));
        assertFalse(stored.getExpiresAt().isBefore(startedAt.plusMinutes(5)));
        assertFalse(stored.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(5)));
        verify(sender).send("alice@example.test", code);
        verifyNoInteractions(refreshTokenRepository);
        assertTrue(passwordEncoder.matches("old-password", alice.getPassword()));
    }

    @Test
    void unknownUserAndUnverifiedAccountDoNotReceiveCodes() {
        service.requestOtp(forgot("missing-user"));
        alice.setStatus(UserStatus.PENDING);
        service.requestOtp(forgot("alice"));

        verify(sender, times(2)).checkAvailable();
        verify(sender, never()).send(anyString(), anyString());
        verifyNoInteractions(otpRepository, refreshTokenRepository);
    }

    @Test
    void missingMailConfigurationFailsBeforeLookingUpKnownOrUnknownAccount() {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email service is not configured"))
                .when(sender).checkAvailable();

        for (String userName : new String[]{"alice", "missing-user"}) {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> service.requestOtp(forgot(userName)));
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        }

        verify(sender, never()).send(anyString(), anyString());
        verifyNoInteractions(userRepository, otpRepository, refreshTokenRepository);
    }

    @Test
    void deliveryFailureDoesNotCreateChallengeOrExposeAccountExistence() {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email delivery failed"))
                .when(sender).send(anyString(), anyString());

        assertDoesNotThrow(() -> service.requestOtp(forgot("alice")));
        assertDoesNotThrow(() -> service.requestOtp(forgot("missing-user")));

        assertNull(storedOtp.get());
        verify(otpRepository, never()).save(any(PasswordResetOtp.class));
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void failedResendPreservesExistingCodeExpiryAndAttemptCounter() {
        service.requestOtp(forgot("alice"));
        storedOtp.get().setRequestedAt(LocalDateTime.now().minusSeconds(61));
        storedOtp.get().setFailedAttempts(4);
        String firstHash = storedOtp.get().getOtpHash();
        LocalDateTime firstRequestedAt = storedOtp.get().getRequestedAt();
        LocalDateTime firstExpiry = storedOtp.get().getExpiresAt();
        clearInvocations(otpRepository);
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email delivery failed"))
                .when(sender).send(anyString(), anyString());

        assertDoesNotThrow(() -> service.requestOtp(forgot("alice")));

        assertEquals(firstHash, storedOtp.get().getOtpHash());
        assertEquals(firstRequestedAt, storedOtp.get().getRequestedAt());
        assertEquals(firstExpiry, storedOtp.get().getExpiresAt());
        assertEquals(4, storedOtp.get().getFailedAttempts());
        assertFalse(storedOtp.get().isConsumed());
        verify(otpRepository, never()).save(any(PasswordResetOtp.class));
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void repeatRequestDuringCooldownDoesNotReplaceCodeOrSendAnotherEmail() {
        service.requestOtp(forgot("alice"));
        String firstHash = storedOtp.get().getOtpHash();
        LocalDateTime firstExpiry = storedOtp.get().getExpiresAt();

        service.requestOtp(forgot("alice"));

        assertEquals(1, deliveredCodes.size());
        assertEquals(firstHash, storedOtp.get().getOtpHash());
        assertEquals(firstExpiry, storedOtp.get().getExpiresAt());
    }

    @Test
    void resendAfterCooldownReplacesChallengeAndResetsAttemptCounter() {
        service.requestOtp(forgot("alice"));
        String firstCode = latestCode();
        String firstHash = storedOtp.get().getOtpHash();
        storedOtp.get().setRequestedAt(LocalDateTime.now().minusSeconds(61));
        storedOtp.get().setFailedAttempts(4);

        service.requestOtp(forgot("alice"));

        assertEquals(2, deliveredCodes.size());
        assertNotEquals(firstHash, storedOtp.get().getOtpHash());
        assertTrue(passwordEncoder.matches(latestCode(), storedOtp.get().getOtpHash()));
        assertEquals(0, storedOtp.get().getFailedAttempts());
        assertFalse(storedOtp.get().isConsumed());
        assertInvalid(reset("alice", firstCode, "new-password"));
        assertTrue(passwordEncoder.matches("old-password", alice.getPassword()));
    }

    @Test
    void validOtpChangesPasswordConsumesChallengeAndRevokesAllSessions() {
        service.requestOtp(forgot("alice"));
        String originalPassword = alice.getPassword();

        service.resetPassword(reset("alice", latestCode(), "new-password"));

        assertNotEquals(originalPassword, alice.getPassword());
        assertNotEquals("new-password", alice.getPassword());
        assertTrue(passwordEncoder.matches("new-password", alice.getPassword()));
        assertFalse(passwordEncoder.matches("old-password", alice.getPassword()));
        assertTrue(storedOtp.get().isConsumed());
        verify(refreshTokenRepository).revokeByUserId(alice.getId());
    }

    @Test
    void passwordResetRecoversLockedAccountAndClearsLoginFailures() {
        alice.setStatus(UserStatus.LOCKED);
        alice.setLockUntill(LocalDateTime.now().plusMinutes(15));
        alice.setFailedLoginAttemps(5);
        service.requestOtp(forgot("alice"));

        service.resetPassword(reset("alice", latestCode(), "new-password"));

        assertEquals(UserStatus.ACTIVE, alice.getStatus());
        assertNull(alice.getLockUntill());
        assertEquals(0, alice.getFailedLoginAttemps());
        assertTrue(passwordEncoder.matches("new-password", alice.getPassword()));
    }

    @Test
    void consumedOtpCannotChangePasswordAgain() {
        service.requestOtp(forgot("alice"));
        String code = latestCode();
        service.resetPassword(reset("alice", code, "new-password"));

        assertInvalid(reset("alice", code, "attacker-password"));

        assertTrue(passwordEncoder.matches("new-password", alice.getPassword()));
        verify(refreshTokenRepository, times(1)).revokeByUserId(alice.getId());
    }

    @Test
    void wrongOtpRecordsAttemptWithoutChangingPasswordOrRevokingSessions() {
        service.requestOtp(forgot("alice"));

        assertInvalid(reset("alice", wrongCode(), "new-password"));

        assertEquals(1, storedOtp.get().getFailedAttempts());
        assertFalse(storedOtp.get().isConsumed());
        assertTrue(passwordEncoder.matches("old-password", alice.getPassword()));
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void correctOtpCanStillSucceedAfterFewerThanFiveFailures() {
        service.requestOtp(forgot("alice"));
        for (int attempt = 0; attempt < 4; attempt++) {
            assertInvalid(reset("alice", wrongCode(), "new-password"));
        }

        service.resetPassword(reset("alice", latestCode(), "new-password"));

        assertTrue(passwordEncoder.matches("new-password", alice.getPassword()));
        assertTrue(storedOtp.get().isConsumed());
    }

    @Test
    void fiveWrongAttemptsInvalidateEvenTheCorrectCode() {
        service.requestOtp(forgot("alice"));
        for (int attempt = 0; attempt < 5; attempt++) {
            assertInvalid(reset("alice", wrongCode(), "new-password"));
        }
        assertEquals(5, storedOtp.get().getFailedAttempts());

        assertInvalid(reset("alice", latestCode(), "new-password"));

        assertTrue(passwordEncoder.matches("old-password", alice.getPassword()));
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void expiredOtpCannotChangePassword() {
        service.requestOtp(forgot("alice"));
        storedOtp.get().setExpiresAt(LocalDateTime.now().minusSeconds(1));

        assertInvalid(reset("alice", latestCode(), "new-password"));

        assertTrue(passwordEncoder.matches("old-password", alice.getPassword()));
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void missingChallengeUnknownUserAndUnverifiedUserHaveSameOtpError() {
        assertInvalid(reset("alice", "123456", "new-password"));
        assertInvalid(reset("missing-user", "123456", "new-password"));
        service.requestOtp(forgot("alice"));
        alice.setStatus(UserStatus.PENDING);
        assertInvalid(reset("alice", latestCode(), "new-password"));

        assertTrue(passwordEncoder.matches("old-password", alice.getPassword()));
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void codeForOneUserCannotResetAnotherUserPassword() {
        service.requestOtp(forgot("alice"));
        User bob = User.builder().Id(12L).username("bob").status(UserStatus.ACTIVE)
                .email("bob@example.test").password(passwordEncoder.encode("bob-password")).build();
        when(userRepository.findByUsernameForUpdate("bob")).thenReturn(Optional.of(bob));

        assertInvalid(reset("bob", latestCode(), "new-password"));

        assertTrue(passwordEncoder.matches("bob-password", bob.getPassword()));
        assertFalse(storedOtp.get().isConsumed());
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void passwordExceedingBcryptUtf8LimitIsRejectedWithoutConsumingOtp() {
        service.requestOtp(forgot("alice"));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.resetPassword(reset("alice", latestCode(), "é".repeat(37))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertFalse(storedOtp.get().isConsumed());
        assertEquals(0, storedOtp.get().getFailedAttempts());
        assertTrue(passwordEncoder.matches("old-password", alice.getPassword()));
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void passwordExactlyAtBcryptUtf8LimitIsAccepted() {
        service.requestOtp(forgot("alice"));
        String password = "é".repeat(36);

        service.resetPassword(reset("alice", latestCode(), password));

        assertTrue(passwordEncoder.matches(password, alice.getPassword()));
        assertTrue(storedOtp.get().isConsumed());
    }

    private void assertInvalid(ResetPasswordRequest request) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.resetPassword(request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Invalid or expired OTP", exception.getReason());
    }

    private String latestCode() {
        return deliveredCodes.get(deliveredCodes.size() - 1);
    }

    private String wrongCode() {
        return "000000".equals(latestCode()) ? "000001" : "000000";
    }

    private ForgotPasswordRequest forgot(String userName) {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUserName(userName);
        return request;
    }

    private ResetPasswordRequest reset(String userName, String code, String newPassword) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setUserName(userName);
        request.setOtpCode(code);
        request.setNewPassword(newPassword);
        return request;
    }
}
