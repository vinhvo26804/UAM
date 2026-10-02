package com.example.demo.service;

import com.example.demo.dto.request.ChangePasswordRequest;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.RefreshTokenRequest;
import com.example.demo.dto.request.VertifyRequest;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.OTP;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.respository.RefreshTokenRepository;
import com.example.demo.respository.UserRepository;
import com.example.demo.respository.VerifyRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTests {

    private static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWYwMTIzNDU2Nzg5YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZg==";

    private AuthService authService;
    private JwtService jwtService;
    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private VerifyRepository verifyRepository;
    private AuthenticationManager authenticationManager;
    private User activeUser;

    @BeforeEach
    void setUp() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expAccess", 360_000L);
        ReflectionTestUtils.setField(jwtService, "expRefresh", 1_296_000_000L);

        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        verifyRepository = mock(VerifyRepository.class);
        authenticationManager = mock(AuthenticationManager.class);
        authService = new AuthService();
        ReflectionTestUtils.setField(authService, "userRepository", userRepository);
        ReflectionTestUtils.setField(authService, "refreshTokenRepository", refreshTokenRepository);
        ReflectionTestUtils.setField(authService, "verifyRespository", verifyRepository);
        ReflectionTestUtils.setField(authService, "authenticationManager", authenticationManager);
        ReflectionTestUtils.setField(authService, "jwtService", jwtService);
        ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);

        activeUser = User.builder()
                .Id(1L)
                .username("alice")
                .email("alice@example.test")
                .password(passwordEncoder.encode("correct-password"))
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findByUsernameForUpdate("alice")).thenReturn(Optional.of(activeUser));
        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> {
            Authentication authentication = invocation.getArgument(0);
            if (!passwordEncoder.matches(authentication.getCredentials().toString(), activeUser.getPassword())) {
                throw new BadCredentialsException("Bad credentials");
            }
            return authentication;
        });
    }

    @Test
    void loginPersistsOneRefreshTokenDigestWithItsActualExpiry() throws Exception {
        LoginResponse response = authService.loginWithJwt(loginRequest("correct-password"));

        assertEquals("access", jwtService.extractTokenType(response.getAccessToken()));
        assertEquals("refresh", jwtService.extractTokenType(response.getRefreshToken()));
        assertEquals("alice", jwtService.extractUserName(response.getRefreshToken()));
        assertTrue(response.getRefreshToken().getBytes(StandardCharsets.UTF_8).length > 72,
                "Use a real JWT exceeding BCrypt's input limit to cover the login failure");

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        verifyNoMoreInteractions(refreshTokenRepository);
        verify(authenticationManager).authenticate(any(Authentication.class));

        RefreshToken stored = tokenCaptor.getValue();
        String expectedDigest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(response.getRefreshToken().getBytes(StandardCharsets.UTF_8)));
        assertEquals(expectedDigest, stored.getHashToken());
        assertTrue(stored.getHashToken().matches("[0-9a-f]{64}"));
        assertNotEquals(response.getRefreshToken(), stored.getHashToken());
        assertSame(activeUser, stored.getUser());
        assertFalse(stored.isRevoke());
        assertEquals(LocalDateTime.ofInstant(
                jwtService.extractAllClaims(response.getRefreshToken()).getExpiration().toInstant(),
                ZoneId.systemDefault()), stored.getExpireAt());
        assertTrue(stored.getExpireAt().isAfter(LocalDateTime.now().plusDays(14)));
        assertTrue(stored.getExpireAt().isBefore(LocalDateTime.now().plusDays(16)));
    }

    @Test
    void wrongPasswordDoesNotPersistRefreshToken() {
        assertLoginStatus("wrong-password", HttpStatus.UNAUTHORIZED);

        assertEquals(1, activeUser.getFailedLoginAttemps());
        assertEquals(UserStatus.ACTIVE, activeUser.getStatus());
        verify(userRepository).save(activeUser);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void inactiveUserDoesNotPersistRefreshToken() {
        activeUser.setStatus(UserStatus.PENDING);

        assertLoginStatus("correct-password", HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(authenticationManager, refreshTokenRepository);
    }

    @Test
    void unknownUserDoesNotPersistRefreshToken() {
        when(userRepository.findByUsernameForUpdate("alice")).thenReturn(Optional.empty());

        assertLoginStatus("correct-password", HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(authenticationManager, refreshTokenRepository);
    }

    @Test
    void disabledAccountWithExpiredLockCannotBeReactivatedByLogin() {
        LocalDateTime expiredLock = LocalDateTime.now().minusMinutes(1);
        activeUser.setStatus(UserStatus.DISABLED);
        activeUser.setLockUntill(expiredLock);
        activeUser.setFailedLoginAttemps(5);

        assertLoginStatus("correct-password", HttpStatus.UNAUTHORIZED);

        assertEquals(UserStatus.DISABLED, activeUser.getStatus());
        assertEquals(expiredLock, activeUser.getLockUntill());
        assertEquals(5, activeUser.getFailedLoginAttemps());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(authenticationManager, refreshTokenRepository);
    }

    @Test
    void manualLockWithoutDeadlineBlocksLoginIndefinitely() {
        activeUser.setStatus(UserStatus.LOCKED);
        activeUser.setLockUntill(null);

        assertLoginStatus("correct-password", HttpStatus.LOCKED);

        assertEquals(UserStatus.LOCKED, activeUser.getStatus());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(authenticationManager, refreshTokenRepository);
    }

    @Test
    void timedLockBeforeDeadlineBlocksLogin() {
        activeUser.setStatus(UserStatus.LOCKED);
        activeUser.setLockUntill(LocalDateTime.now().plusMinutes(10));

        assertLoginStatus("correct-password", HttpStatus.LOCKED);

        assertEquals(UserStatus.LOCKED, activeUser.getStatus());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(authenticationManager, refreshTokenRepository);
    }

    @Test
    void expiredTimedLockAllowsLoginAndResetsLockMetadata() {
        activeUser.setStatus(UserStatus.LOCKED);
        activeUser.setLockUntill(LocalDateTime.now().minusMinutes(1));
        activeUser.setFailedLoginAttemps(5);

        LoginResponse response = authService.loginWithJwt(loginRequest("correct-password"));

        assertEquals(UserStatus.ACTIVE, activeUser.getStatus());
        assertNull(activeUser.getLockUntill());
        assertEquals(0, activeUser.getFailedLoginAttemps());
        assertEquals("alice", jwtService.extractUserName(response.getAccessToken()));
        verify(userRepository).save(activeUser);
    }

    @Test
    void expiredTimedLockWithWrongPasswordStartsANewFailureCount() {
        activeUser.setStatus(UserStatus.LOCKED);
        activeUser.setLockUntill(LocalDateTime.now().minusMinutes(1));
        activeUser.setFailedLoginAttemps(5);

        assertLoginStatus("wrong-password", HttpStatus.UNAUTHORIZED);

        assertEquals(UserStatus.ACTIVE, activeUser.getStatus());
        assertNull(activeUser.getLockUntill());
        assertEquals(1, activeUser.getFailedLoginAttemps());
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void fifthFailedPasswordLocksAccountAndRevokesExistingSessions() {
        activeUser.setFailedLoginAttemps(4);

        assertLoginStatus("wrong-password", HttpStatus.UNAUTHORIZED);

        assertEquals(UserStatus.LOCKED, activeUser.getStatus());
        assertEquals(5, activeUser.getFailedLoginAttemps());
        assertTrue(activeUser.getLockUntill().isAfter(LocalDateTime.now().plusMinutes(14)));
        assertTrue(activeUser.getLockUntill().isBefore(LocalDateTime.now().plusMinutes(16)));
        verify(userRepository).save(activeUser);
        verify(refreshTokenRepository).revokeByUserId(activeUser.getId());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"PENDING", "LOCKED", "DISABLED"})
    void legacyLoginCannotBypassAccountStatus(UserStatus status) {
        activeUser.setStatus(status);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.Login(loginRequest("correct-password")));

        assertEquals(status == UserStatus.LOCKED ? HttpStatus.LOCKED : HttpStatus.UNAUTHORIZED,
                exception.getStatusCode());
        assertEquals(status, activeUser.getStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "LOCKED", "DISABLED"})
    void verificationCannotReactivateOrModifyAnAccountThatIsNotPending(UserStatus status) {
        activeUser.setStatus(status);
        activeUser.setLockUntill(LocalDateTime.now().minusMinutes(1));
        activeUser.setFailedLoginAttemps(5);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.Vertify(verificationRequest("123456")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(status, activeUser.getStatus());
        assertEquals(5, activeUser.getFailedLoginAttemps());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(verifyRepository);
    }

    @Test
    void successfulVerificationActivatesPendingAccountAndConsumesOtp() {
        activeUser.setStatus(UserStatus.PENDING);
        OTP otp = OTP.builder().userId(activeUser.getId()).purpose("verify")
                .otpCode("123456").expriedAt(LocalDateTime.now().plusMinutes(5)).build();
        when(verifyRepository.findByUserIdAndPurpose(activeUser.getId(), "verify"))
                .thenReturn(Optional.of(otp));

        assertEquals("Verify Success", authService.Vertify(verificationRequest("123456")));

        assertEquals(UserStatus.ACTIVE, activeUser.getStatus());
        verify(userRepository).save(activeUser);
        verify(verifyRepository).delete(otp);
        ResponseStatusException replay = assertThrows(ResponseStatusException.class,
                () -> authService.Vertify(verificationRequest("123456")));
        assertEquals(HttpStatus.UNAUTHORIZED, replay.getStatusCode());
        verify(verifyRepository).findByUserIdAndPurpose(activeUser.getId(), "verify");
        verifyNoMoreInteractions(verifyRepository);
    }

    @Test
    void refreshTokensForSameUserHaveIndependentSessionIdentifiers() {
        String first = jwtService.generateRefreshToken("alice");
        String second = jwtService.generateRefreshToken("alice");

        String firstId = jwtService.extractAllClaims(first).getId();
        String secondId = jwtService.extractAllClaims(second).getId();
        assertTrue(firstId != null && !firstId.isBlank());
        assertTrue(secondId != null && !secondId.isBlank());
        assertNotEquals(firstId, secondId);
        assertNotEquals(first, second);
    }

    @Test
    void persistedRefreshTokenReturnsAccessTokenForItsActiveOwner() throws Exception {
        String token = jwtService.generateRefreshToken("alice");
        RefreshToken stored = storedToken(token, activeUser);
        when(refreshTokenRepository.findFirstByHashTokenAndRevokeFalseAndExpireAtAfter(
                eq(digest(token)), any(LocalDateTime.class))).thenReturn(Optional.of(stored));

        LoginResponse response = authService.refreshToken(new RefreshTokenRequest(token));

        assertEquals("access", jwtService.extractTokenType(response.getAccessToken()));
        assertEquals("alice", jwtService.extractUserName(response.getAccessToken()));
        assertEquals(token, response.getRefreshToken());
        verify(refreshTokenRepository).findFirstByHashTokenAndRevokeFalseAndExpireAtAfter(
                eq(digest(token)), any(LocalDateTime.class));
        verifyNoMoreInteractions(refreshTokenRepository);
    }

    @Test
    void signedButUnpersistedRefreshTokenIsUnauthorized() {
        String token = jwtService.generateRefreshToken("alice");

        assertRefreshUnauthorized(token);
    }

    @Test
    void refreshTokenForInactiveOwnerIsUnauthorized() throws Exception {
        String token = jwtService.generateRefreshToken("alice");
        activeUser.setStatus(UserStatus.PENDING);
        when(refreshTokenRepository.findFirstByHashTokenAndRevokeFalseAndExpireAtAfter(
                eq(digest(token)), any(LocalDateTime.class)))
                .thenReturn(Optional.of(storedToken(token, activeUser)));

        assertRefreshUnauthorized(token);
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"LOCKED", "DISABLED"})
    void refreshCannotUnlockOrReactivateAccountWithAnExpiredLock(UserStatus status) throws Exception {
        String token = jwtService.generateRefreshToken("alice");
        activeUser.setStatus(status);
        LocalDateTime expiredLock = LocalDateTime.now().minusMinutes(1);
        activeUser.setLockUntill(expiredLock);
        when(refreshTokenRepository.findFirstByHashTokenAndRevokeFalseAndExpireAtAfter(
                eq(digest(token)), any(LocalDateTime.class)))
                .thenReturn(Optional.of(storedToken(token, activeUser)));

        assertRefreshUnauthorized(token);

        assertEquals(status, activeUser.getStatus());
        assertEquals(expiredLock, activeUser.getLockUntill());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void refreshTokenWithDifferentDatabaseOwnerIsUnauthorized() throws Exception {
        String token = jwtService.generateRefreshToken("alice");
        User otherUser = User.builder().username("bob").status(UserStatus.ACTIVE).build();
        when(refreshTokenRepository.findFirstByHashTokenAndRevokeFalseAndExpireAtAfter(
                eq(digest(token)), any(LocalDateTime.class)))
                .thenReturn(Optional.of(storedToken(token, otherUser)));

        assertRefreshUnauthorized(token);
    }

    @Test
    void invalidExpiredOrIncompleteJwtCannotReachRefreshTokenLookup() {
        assertRefreshUnauthorized("invalid.jwt.token");
        assertRefreshUnauthorized(jwtService.generateAccessToken("alice"));
        assertRefreshUnauthorized(signedRefreshToken("alice", new Date(System.currentTimeMillis() - 60_000)));
        assertRefreshUnauthorized(signedRefreshToken(null, new Date(System.currentTimeMillis() + 60_000)));
        assertRefreshUnauthorized(signedRefreshToken("alice", null));
        assertRefreshUnauthorized(Jwts.builder()
                .subject("alice")
                .claim("type", 42)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .compact());

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void logoutRevokesByDigestEvenWhenTokenIsNotAValidJwt() throws Exception {
        String token = "expired-or-unknown-refresh-token";

        authService.logout(new RefreshTokenRequest(token));

        verify(refreshTokenRepository).revokeByHashToken(digest(token));
        verifyNoMoreInteractions(refreshTokenRepository);
    }

    @Test
    void logoutCanRevokeExpiredRefreshToken() throws Exception {
        String token = signedRefreshToken("alice", new Date(System.currentTimeMillis() - 60_000));

        authService.logout(new RefreshTokenRequest(token));

        verify(refreshTokenRepository).revokeByHashToken(digest(token));
        verifyNoMoreInteractions(refreshTokenRepository);
    }

    @Test
    void activeUserCanChangePasswordWhileHoldingTheAccountRowLock() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "correct-password", "new-password", "new-password");

        authService.changePassword("alice", request);

        assertTrue(new BCryptPasswordEncoder().matches("new-password", activeUser.getPassword()));
        assertEquals(UserStatus.ACTIVE, activeUser.getStatus());
        verify(userRepository).findByUsernameForUpdate("alice");
        verify(userRepository).save(activeUser);
        verify(userRepository, never()).findByUsername(any());
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"PENDING", "LOCKED", "DISABLED"})
    void passwordChangeCannotModifyOrReactivateInactiveAccounts(UserStatus status) {
        activeUser.setStatus(status);
        activeUser.setFailedLoginAttemps(5);
        LocalDateTime expiredLock = LocalDateTime.now().minusMinutes(1);
        activeUser.setLockUntill(expiredLock);
        String originalPassword = activeUser.getPassword();
        ChangePasswordRequest request = new ChangePasswordRequest(
                "correct-password", "new-password", "new-password");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.changePassword("alice", request));

        assertEquals(status == UserStatus.LOCKED ? HttpStatus.LOCKED : HttpStatus.UNAUTHORIZED,
                exception.getStatusCode());
        assertEquals(status, activeUser.getStatus());
        assertEquals(originalPassword, activeUser.getPassword());
        assertEquals(expiredLock, activeUser.getLockUntill());
        assertEquals(5, activeUser.getFailedLoginAttemps());
        verify(userRepository).findByUsernameForUpdate("alice");
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).findByUsername(any());
    }

    private void assertRefreshUnauthorized(String token) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.refreshToken(new RefreshTokenRequest(token)));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    private void assertLoginStatus(String password, HttpStatus status) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.loginWithJwt(loginRequest(password)));
        assertEquals(status, exception.getStatusCode());
    }

    private VertifyRequest verificationRequest(String code) {
        VertifyRequest request = new VertifyRequest();
        request.setUserName("alice");
        request.setOtpCode(code);
        return request;
    }

    private RefreshToken storedToken(String token, User user) throws Exception {
        RefreshToken stored = new RefreshToken();
        stored.setHashToken(digest(token));
        stored.setUser(user);
        stored.setRevoke(false);
        stored.setExpireAt(LocalDateTime.now().plusDays(1));
        return stored;
    }

    private String signedRefreshToken(String subject, Date expiration) {
        return Jwts.builder()
                .subject(subject)
                .claim("type", "refresh")
                .expiration(expiration)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .compact();
    }

    private String digest(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    private LoginRequest loginRequest(String password) {
        LoginRequest request = new LoginRequest();
        request.setUserName("alice");
        request.setPassword(password);
        return request;
    }
}
