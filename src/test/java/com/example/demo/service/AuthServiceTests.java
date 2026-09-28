package com.example.demo.service;

import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.RefreshTokenRequest;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.User;
import com.example.demo.respository.RefreshTokenRepository;
import com.example.demo.respository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
        authenticationManager = mock(AuthenticationManager.class);
        authService = new AuthService();
        ReflectionTestUtils.setField(authService, "userRepository", userRepository);
        ReflectionTestUtils.setField(authService, "refreshTokenRepository", refreshTokenRepository);
        ReflectionTestUtils.setField(authService, "authenticationManager", authenticationManager);
        ReflectionTestUtils.setField(authService, "jwtService", jwtService);
        ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);

        activeUser = User.builder()
                .username("alice")
                .email("alice@example.test")
                .password(passwordEncoder.encode("correct-password"))
                .status("ACTIVE")
                .build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser));
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
        assertThrows(BadCredentialsException.class,
                () -> authService.loginWithJwt(loginRequest("wrong-password")));

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void inactiveUserDoesNotPersistRefreshToken() {
        activeUser.setStatus("PENDING");

        assertThrows(RuntimeException.class,
                () -> authService.loginWithJwt(loginRequest("correct-password")));

        verifyNoInteractions(authenticationManager, refreshTokenRepository);
    }

    @Test
    void unknownUserDoesNotPersistRefreshToken() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> authService.loginWithJwt(loginRequest("correct-password")));

        verifyNoInteractions(authenticationManager, refreshTokenRepository);
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
        activeUser.setStatus("PENDING");
        when(refreshTokenRepository.findFirstByHashTokenAndRevokeFalseAndExpireAtAfter(
                eq(digest(token)), any(LocalDateTime.class)))
                .thenReturn(Optional.of(storedToken(token, activeUser)));

        assertRefreshUnauthorized(token);
    }

    @Test
    void refreshTokenWithDifferentDatabaseOwnerIsUnauthorized() throws Exception {
        String token = jwtService.generateRefreshToken("alice");
        User otherUser = User.builder().username("bob").status("ACTIVE").build();
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

    private void assertRefreshUnauthorized(String token) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.refreshToken(new RefreshTokenRequest(token)));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
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
