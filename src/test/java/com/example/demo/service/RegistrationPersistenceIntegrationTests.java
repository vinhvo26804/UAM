package com.example.demo.service;

import com.example.demo.dto.request.RegisterRequest;
import com.example.demo.entity.OTP;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.respository.UserRepository;
import com.example.demo.respository.VerifyRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false"
})
@Transactional
class RegistrationPersistenceIntegrationTests {
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VerifyRepository verifyRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    private String testUsername;
    private Long testUserId;

    @Test
    void registrationPersistsDefaultUserRoleWithoutOptionalProfileFields() {
        testUsername = "register-" + UUID.randomUUID();
        RegisterRequest request = new RegisterRequest();
        request.setUserName(testUsername);
        request.setEmail(testUsername + "@example.test");
        request.setPassword("registration-test-password");
        LocalDateTime startedAt = LocalDateTime.now();

        assertEquals("Register Success", authService.register(request));
        entityManager.flush();
        entityManager.clear();

        User persisted = userRepository.findByUsername(testUsername).orElseThrow();
        testUserId = persisted.getId();
        assertEquals(request.getEmail(), persisted.getEmail());
        assertEquals(UserStatus.PENDING, persisted.getStatus());
        assertNotEquals(request.getPassword(), persisted.getPassword());
        assertTrue(passwordEncoder.matches(request.getPassword(), persisted.getPassword()));
        assertNull(persisted.getFullname());
        assertNull(persisted.getDoB());
        assertNull(persisted.getPhone());
        assertEquals(Set.of(Role.RoleType.USER), persisted.getRoles().stream()
                .map(Role::getName).collect(Collectors.toSet()));
        assertEquals(List.of("USER"), jdbc.queryForList("""
                select r.name from user_roles ur
                join role r on r.id = ur.role_id
                where ur.user_id = ?
                """, String.class, testUserId));

        OTP verification = verifyRepository.findByUserIdAndPurpose(testUserId, "verify").orElseThrow();
        assertTrue(verification.getOtpCode().matches("[0-9]{6}"));
        assertTrue(verification.getExpriedAt().isAfter(startedAt));
        assertFalse(verification.getExpriedAt().isAfter(LocalDateTime.now().plusMinutes(5)));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from otp where user_id = ? and purpose = 'verify'",
                Integer.class, testUserId));
    }

    @AfterTransaction
    void registrationTestDataWasRolledBack() {
        if (testUsername != null) {
            assertEquals(0, jdbc.queryForObject(
                    "select count(*) from users where username = ?", Integer.class, testUsername));
        }
        if (testUserId != null) {
            assertEquals(0, jdbc.queryForObject(
                    "select count(*) from user_roles where user_id = ?", Integer.class, testUserId));
            assertEquals(0, jdbc.queryForObject(
                    "select count(*) from otp where user_id = ?", Integer.class, testUserId));
        }
    }
}
