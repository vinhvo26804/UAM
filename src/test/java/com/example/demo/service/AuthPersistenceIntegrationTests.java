package com.example.demo.service;

import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.RefreshTokenRequest;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.respository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false"
})
@Transactional
class AuthPersistenceIntegrationTests {
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    private Long testUserId;

    @Test
    void successfulLoginInsertsRefreshTokenIntoDatabase() {
        LoginRequest request = createActiveUser();
        LoginResponse response = authService.loginWithJwt(request);
        entityManager.flush();
        entityManager.clear();

        assertTrue(jwtService.isTokenValid(response.getAccessToken()));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from refresh_token where user_id = ?", Integer.class, testUserId));
        var row = jdbc.queryForMap(
                "select hash_token, expire_at, revoke from refresh_token where user_id = ?", testUserId);
        String storedHash = (String) row.get("hash_token");
        assertNotEquals(response.getRefreshToken(), storedHash);
        assertTrue(storedHash.matches("[0-9a-f]{64}"));
        assertEquals(Boolean.FALSE, row.get("revoke"));
        LocalDateTime expiration = ((Timestamp) row.get("expire_at")).toLocalDateTime();
        assertTrue(expiration.isAfter(LocalDateTime.now()));
        assertEquals(LocalDateTime.ofInstant(
                jwtService.extractAllClaims(response.getRefreshToken()).getExpiration().toInstant(),
                ZoneId.systemDefault()), expiration);
    }

    @Test
    void logoutRevokesOnlySelectedSessionAndRepeatedLogoutIsHarmless() {
        LoginRequest login = createActiveUser();
        LoginResponse firstSession = authService.loginWithJwt(login);
        LoginResponse secondSession = authService.loginWithJwt(login);
        entityManager.flush();
        entityManager.clear();
        assertNotEquals(firstSession.getRefreshToken(), secondSession.getRefreshToken());

        RefreshTokenRequest firstRefresh = new RefreshTokenRequest(firstSession.getRefreshToken());
        assertEquals(login.getUserName(), jwtService.extractUserName(
                authService.refreshToken(firstRefresh).getAccessToken()));

        authService.logout(firstRefresh);
        authService.logout(firstRefresh);
        authService.logout(new RefreshTokenRequest("unknown-token"));
        entityManager.flush();
        entityManager.clear();

        assertEquals(2, jdbc.queryForObject(
                "select count(*) from refresh_token where user_id = ?", Integer.class, testUserId));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from refresh_token where user_id = ? and revoke = true",
                Integer.class, testUserId));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from refresh_token where user_id = ? and revoke = false",
                Integer.class, testUserId));
        assertEquals(login.getUserName(), jwtService.extractUserName(authService.refreshToken(
                new RefreshTokenRequest(secondSession.getRefreshToken())).getAccessToken()));

        assertRefreshUnauthorized(firstSession.getRefreshToken());
    }

    @Test
    void logoutRevokesAllLegacyRowsSharingTheSameTokenHash() {
        LoginResponse session = authService.loginWithJwt(createActiveUser());
        entityManager.flush();
        entityManager.clear();
        jdbc.update("""
                insert into refresh_token (hash_token, expire_at, revoke, user_id)
                select hash_token, expire_at, revoke, user_id from refresh_token where user_id = ?
                """, testUserId);

        assertEquals(2, jdbc.queryForObject(
                "select count(*) from refresh_token where user_id = ?", Integer.class, testUserId));
        assertNotNull(authService.refreshToken(new RefreshTokenRequest(session.getRefreshToken())).getAccessToken());

        authService.logout(new RefreshTokenRequest(session.getRefreshToken()));
        entityManager.flush();
        entityManager.clear();

        assertEquals(2, jdbc.queryForObject(
                "select count(*) from refresh_token where user_id = ? and revoke = true",
                Integer.class, testUserId));
        assertRefreshUnauthorized(session.getRefreshToken());
    }

    @Test
    void databaseExpirationRejectsOtherwiseValidJwt() {
        LoginResponse session = authService.loginWithJwt(createActiveUser());
        entityManager.flush();
        entityManager.clear();
        jdbc.update("update refresh_token set expire_at = ? where user_id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)), testUserId);

        assertTrue(jwtService.isTokenValid(session.getRefreshToken()));
        assertRefreshUnauthorized(session.getRefreshToken());
    }

    private LoginRequest createActiveUser() {
        String username = "login-test-" + UUID.randomUUID();
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setPassword(passwordEncoder.encode("test-password"));
        user.setStatus(UserStatus.ACTIVE);
        testUserId = userRepository.saveAndFlush(user).getId();
        entityManager.clear();

        LoginRequest request = new LoginRequest();
        request.setUserName(username);
        request.setPassword("test-password");
        return request;
    }

    private void assertRefreshUnauthorized(String token) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.refreshToken(new RefreshTokenRequest(token)));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @AfterTransaction
    void testDataWasRolledBack() {
        if (testUserId != null) {
            assertEquals(0, jdbc.queryForObject(
                    "select count(*) from refresh_token where user_id = ?", Integer.class, testUserId));
            assertEquals(0, jdbc.queryForObject(
                    "select count(*) from users where id = ?", Integer.class, testUserId));
        }
    }
}
