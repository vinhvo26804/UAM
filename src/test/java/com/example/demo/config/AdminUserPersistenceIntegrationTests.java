package com.example.demo.config;

import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.respository.RefreshTokenRepository;
import com.example.demo.respository.RoleRepository;
import com.example.demo.respository.UserRepository;
import com.example.demo.service.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"
})
@Transactional
class AdminUserPersistenceIntegrationTests {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilterChain;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    private final List<Long> testUserIds = new ArrayList<>();
    private MockMvc mockMvc;
    private User admin;
    private User member;
    private String adminAuthorization;
    private String memberAuthorization;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(securityFilterChain)
                .build();
        String prefix = "admin-test-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        admin = createUser(prefix + "-admin", Role.RoleType.ADMIN, UserStatus.ACTIVE);
        member = createUser(prefix + "-member", Role.RoleType.USER, UserStatus.ACTIVE);
        adminAuthorization = "Bearer " + jwtService.generateAccessToken(admin.getUsername());
        memberAuthorization = "Bearer " + jwtService.generateAccessToken(member.getUsername());
        flushAndClear();
    }

    @Test
    void adminUpdatesProfileThenSoftDeletesWithoutRemovingUserOrRoles() throws Exception {
        createRefreshToken(member);
        String updatedEmail = "updated-" + member.getEmail();

        mockMvc.perform(put("/api/admin/users/{id}", member.getId())
                        .header("Authorization", adminAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","fullname":"Updated User",
                                 "doB":"1999-02-03","phone":"0901234567"}
                                """.formatted(updatedEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(updatedEmail))
                .andExpect(jsonPath("$.fullname").value("Updated User"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist());
        flushAndClear();
        var updated = jdbc.queryForMap("select email, fullname, phone, dob, password from users where id = ?",
                member.getId());
        assertEquals(updatedEmail, updated.get("email"));
        assertEquals("Updated User", updated.get("fullname"));
        assertEquals("0901234567", updated.get("phone"));
        assertEquals(java.sql.Date.valueOf("1999-02-03"), updated.get("dob"));
        assertEquals(member.getPassword(), updated.get("password"));
        assertEquals(1, countRefreshTokens(member, true));

        createRefreshToken(member);
        mockMvc.perform(delete("/api/admin/users/{id}", member.getId())
                        .header("Authorization", adminAuthorization))
                .andExpect(status().isNoContent());
        assertPersistedStatus(member, UserStatus.DISABLED);
        assertEquals(1, jdbc.queryForObject("select count(*) from users where id = ?",
                Integer.class, member.getId()));
        assertEquals(1, jdbc.queryForObject("select count(*) from user_roles where user_id = ?",
                Integer.class, member.getId()));
        assertEquals(2, countRefreshTokens(member, true));
        assertEquals(0, countRefreshTokens(member, false));
        mockMvc.perform(get("/api/admin/users/{id}", member.getId())
                        .header("Authorization", adminAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
        assertMemberAccess(false);
    }

    @Test
    void manualLockAndUnlockControlExistingAccessTokenWithoutRestoringRefreshToken() throws Exception {
        String refreshToken = createRefreshToken(member);
        assertMemberAccess(true);

        changeStatus(member, "lock", UserStatus.LOCKED);
        assertNull(jdbc.queryForObject("select lock_untill from users where id = ?",
                LocalDateTime.class, member.getId()));
        assertEquals(1, countRefreshTokens(member, true));
        assertMemberAccess(false);

        changeStatus(member, "unlock", UserStatus.ACTIVE);
        assertEquals(0, jdbc.queryForObject("select failed_login_attemps from users where id = ?",
                Integer.class, member.getId()));
        assertMemberAccess(true);
        assertRefreshRejected(refreshToken);
    }

    @Test
    void disableAndActivatePersistStatusAndKeepOldRefreshSessionsRevoked() throws Exception {
        String refreshToken = createRefreshToken(member);

        changeStatus(member, "disable", UserStatus.DISABLED);
        assertMemberAccess(false);
        assertEquals(1, countRefreshTokens(member, true));

        changeStatus(member, "activate", UserStatus.ACTIVE);
        assertMemberAccess(true);
        assertEquals(1, countRefreshTokens(member, true));
        assertRefreshRejected(refreshToken);
    }

    @Test
    void adminCanActivatePendingAccountAndReadItThroughStatusFilter() throws Exception {
        User pending = createUser(member.getUsername() + "-new", Role.RoleType.USER, UserStatus.PENDING);
        flushAndClear();
        String authorization = "Bearer " + jwtService.generateAccessToken(pending.getUsername());
        mockMvc.perform(get("/me").header("Authorization", authorization))
                .andExpect(status().isUnauthorized());

        changeStatus(pending, "activate", UserStatus.ACTIVE);
        mockMvc.perform(get("/me").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", adminAuthorization)
                        .param("username", pending.getUsername())
                        .param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value(pending.getUsername()));
    }

    @Test
    void roleReadsInitializePermissionsAndRemainAvailableToAdmin() throws Exception {
        Role role = roleRepository.findByName(Role.RoleType.USER).orElseThrow();
        assertTrue(entityManager.getEntityManagerFactory().getPersistenceUnitUtil()
                .isLoaded(role, "permissions"));
        flushAndClear();
        for (Role loaded : roleRepository.findAll()) {
            assertTrue(entityManager.getEntityManagerFactory().getPersistenceUnitUtil()
                    .isLoaded(loaded, "permissions"));
        }
        flushAndClear();
        mockMvc.perform(get("/api/roles").header("Authorization", adminAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItems("ADMIN", "USER")));
        mockMvc.perform(get("/api/roles/USER").header("Authorization", adminAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("USER"))
                .andExpect(jsonPath("$.permissions").isArray());
    }

    private User createUser(String username, Role.RoleType roleType, UserStatus userStatus) {
        Role role = roleRepository.findByName(roleType).orElseGet(() -> {
            Role created = new Role();
            created.setName(roleType);
            return roleRepository.save(created);
        });
        User saved = userRepository.saveAndFlush(User.builder()
                .username(username)
                .email(username + "@example.test")
                .password("unused-password-for-admin-persistence-test")
                .fullname("Admin Test User")
                .phone("0123456789")
                .doB(LocalDate.of(2000, 1, 1))
                .status(userStatus)
                .roles(Set.of(role))
                .build());
        testUserIds.add(saved.getId());
        return saved;
    }

    private String createRefreshToken(User user) throws Exception {
        String token = jwtService.generateRefreshToken(user.getUsername());
        RefreshToken stored = new RefreshToken();
        stored.setHashToken(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8))));
        stored.setUser(userRepository.findById(user.getId()).orElseThrow());
        stored.setRevoke(false);
        stored.setExpireAt(LocalDateTime.now().plusDays(1));
        refreshTokenRepository.save(stored);
        flushAndClear();
        return token;
    }

    private void changeStatus(User user, String action, UserStatus expected) throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/{action}", user.getId(), action)
                        .header("Authorization", adminAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expected.name()));
        assertPersistedStatus(user, expected);
    }

    private void assertPersistedStatus(User user, UserStatus expected) {
        flushAndClear();
        assertEquals(expected.name(), jdbc.queryForObject("select status from users where id = ?",
                String.class, user.getId()));
    }

    private int countRefreshTokens(User user, boolean revoked) {
        return jdbc.queryForObject("select count(*) from refresh_token where user_id = ? and revoke = ?",
                Integer.class, user.getId(), revoked);
    }

    private void assertMemberAccess(boolean allowed) throws Exception {
        mockMvc.perform(get("/me").header("Authorization", memberAuthorization))
                .andExpect(allowed ? status().isOk() : status().isUnauthorized());
    }

    private void assertRefreshRejected(String token) throws Exception {
        mockMvc.perform(post("/refreshToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + token + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @AfterTransaction
    void syntheticUsersRolesAndRefreshTokensWereRolledBack() {
        for (Long userId : testUserIds) {
            assertEquals(0, jdbc.queryForObject("select count(*) from refresh_token where user_id = ?",
                    Integer.class, userId));
            assertEquals(0, jdbc.queryForObject("select count(*) from user_roles where user_id = ?",
                    Integer.class, userId));
            assertEquals(0, jdbc.queryForObject("select count(*) from users where id = ?",
                    Integer.class, userId));
        }
    }
}
