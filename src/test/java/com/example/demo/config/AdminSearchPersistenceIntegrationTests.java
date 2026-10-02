package com.example.demo.config;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"
})
@Transactional
class AdminSearchPersistenceIntegrationTests {

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
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<Long> testUserIds = new ArrayList<>();
    private MockMvc mockMvc;
    private String usernamePrefix;
    private User admin;
    private User member;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(securityFilterChain)
                .build();
        usernamePrefix = "search-" + UUID.randomUUID().toString().replace("-", "");
        admin = createUser(usernamePrefix + "-admin", Role.RoleType.ADMIN);
        member = createUser(usernamePrefix + "-member", Role.RoleType.USER);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void persistedAdminCanSearchWithAllFiltersAbsent() throws Exception {
        mockMvc.perform(get("/search").header("Authorization", authorization(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItems(admin.getUsername(), member.getUsername())));
    }

    @Test
    void persistedAdminCanSearchUsingCaseInsensitiveRoleFilter() throws Exception {
        mockMvc.perform(get("/search")
                        .header("Authorization", authorization(admin))
                        .param("username", usernamePrefix)
                        .param("role", " aDmIn "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value(admin.getUsername()))
                .andExpect(jsonPath("$[0].roles[0]").value("ADMIN"));
    }

    @Test
    void blankFiltersAreTreatedAsAbsent() throws Exception {
        mockMvc.perform(get("/search")
                        .header("Authorization", authorization(admin))
                        .param("username", "")
                        .param("email", " ")
                        .param("role", "   ")
                        .param("status", "\t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItems(admin.getUsername(), member.getUsername())));
    }

    @Test
    void usernameEmailAndStatusFiltersIgnoreCaseAndSurroundingWhitespace() throws Exception {
        mockMvc.perform(get("/search")
                        .header("Authorization", authorization(admin))
                        .param("username", " " + admin.getUsername().toUpperCase(Locale.ROOT) + " ")
                        .param("email", " " + admin.getEmail().toUpperCase(Locale.ROOT) + " ")
                        .param("status", " aCtIvE "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value(admin.getUsername()));
    }

    @Test
    void userRoleFilterReturnsOnlyMembers() throws Exception {
        mockMvc.perform(get("/search")
                        .header("Authorization", authorization(admin))
                        .param("username", usernamePrefix)
                        .param("role", " uSeR "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value(member.getUsername()))
                .andExpect(jsonPath("$[0].roles[0]").value("USER"));
    }

    @Test
    void unknownRoleFilterReturnsNoResults() throws Exception {
        mockMvc.perform(get("/search")
                        .header("Authorization", authorization(admin))
                        .param("username", usernamePrefix)
                        .param("role", "UNKNOWN_ROLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void persistedUserIsForbiddenFromSearching() throws Exception {
        mockMvc.perform(get("/search").header("Authorization", authorization(member)))
                .andExpect(status().isForbidden());
    }

    private User createUser(String username, Role.RoleType roleType) {
        Role role = roleRepository.findByName(roleType).orElseGet(() -> {
            Role created = new Role();
            created.setName(roleType);
            return roleRepository.save(created);
        });
        User user = User.builder()
                .username(username)
                .password("unused-password-for-jwt-search-test")
                .email(username + "@example.test")
                .fullname("Search Test")
                .phone("0123456789")
                .doB(LocalDate.of(2000, 1, 1))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role))
                .build();
        User saved = userRepository.saveAndFlush(user);
        testUserIds.add(saved.getId());
        return saved;
    }

    private String authorization(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getUsername());
    }

    @AfterTransaction
    void syntheticUsersAndTheirRolesWereRolledBack() {
        for (Long testUserId : testUserIds) {
            assertEquals(0, jdbc.queryForObject(
                    "select count(*) from user_roles where user_id = ?", Integer.class, testUserId));
            assertEquals(0, jdbc.queryForObject(
                    "select count(*) from users where id = ?", Integer.class, testUserId));
        }
    }
}
