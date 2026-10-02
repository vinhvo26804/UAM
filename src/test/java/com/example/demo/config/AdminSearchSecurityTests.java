package com.example.demo.config;

import com.example.demo.controller.UserController;
import com.example.demo.entity.Permission;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.respository.UserRepository;
import com.example.demo.respository.VerifyRepository;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.JwtService;
import com.example.demo.service.UserService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AdminSearchSecurityTests.TestWebConfiguration.class)
@TestPropertySource(properties = {
        "jwt.secret=" + SecurityIntegrationTests.TEST_SECRET,
        "jwt.expiration=360000",
        "jwt.refresh-expiration=1296000000"
})
class AdminSearchSecurityTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilterChain;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private VerifyRepository verifyRepository;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .Id(1L)
                .username("alice")
                .password("unused")
                .email("alice@example.com")
                .fullname("Alice")
                .phone("0123456789")
                .doB(LocalDate.of(2000, 1, 1))
                .status(UserStatus.ACTIVE)
                .build();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(securityFilterChain)
                .build();
    }

    @Test
    void databaseAdminCanSearchWithoutAnyPermissionRows() throws Exception {
        user.setRoles(Set.of(role(Role.RoleType.ADMIN)));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.searchUsers(null, null, null, null)).thenReturn(List.of(user));

        mockMvc.perform(get("/search").header("Authorization", accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].fullname").value("Alice"))
                .andExpect(jsonPath("$[0].roles[0]").value("ADMIN"));

        verify(userRepository).searchUsers(null, null, null, null);
    }

    @Test
    void databaseUserCannotSearch() throws Exception {
        assertSearchForbidden(Role.RoleType.USER);
    }

    @Test
    void databaseModeratorCannotSearch() throws Exception {
        assertSearchForbidden(Role.RoleType.MODERATOR);
    }

    @Test
    void userWithoutDatabaseRolesCannotSearch() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/search").header("Authorization", accessToken()))
                .andExpect(status().isForbidden());

        verify(userRepository, never()).searchUsers(null, null, null, null);
    }

    @Test
    void searchRequiresAccessToken() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userRepository);
    }

    @Test
    void sameAccessTokenUsesCurrentDatabaseRolesAfterPromotionAndDemotion() throws Exception {
        String authorization = accessToken();
        user.setRoles(Set.of(role(Role.RoleType.USER)));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.searchUsers(null, null, null, null)).thenReturn(List.of(user));

        mockMvc.perform(get("/search").header("Authorization", authorization))
                .andExpect(status().isForbidden());

        user.setRoles(Set.of(role(Role.RoleType.ADMIN)));
        mockMvc.perform(get("/search").header("Authorization", authorization))
                .andExpect(status().isOk());

        user.setRoles(Set.of(role(Role.RoleType.USER)));
        mockMvc.perform(get("/search").header("Authorization", authorization))
                .andExpect(status().isForbidden());

        verify(userRepository, times(3)).findByUsername("alice");
        verify(userRepository).searchUsers(null, null, null, null);
    }

    @Test
    void rolePrefixDoesNotChangeOrDuplicatePermissionAuthorities() {
        Permission permission = new Permission();
        permission.setName("READ_USERS");
        Role admin = role(Role.RoleType.ADMIN);
        admin.getPermissions().add(permission);
        Role member = role(Role.RoleType.USER);
        member.getPermissions().add(permission);
        user.setRoles(Set.of(admin, member));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThat(userDetailsService.loadUserByUsername("alice").getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER", "READ_USERS");
    }

    private void assertSearchForbidden(Role.RoleType roleType) throws Exception {
        user.setRoles(Set.of(role(roleType)));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/search").header("Authorization", accessToken()))
                .andExpect(status().isForbidden());

        verify(userRepository, never()).searchUsers(null, null, null, null);
    }

    private String accessToken() {
        return "Bearer " + jwtService.generateAccessToken("alice");
    }

    private Role role(Role.RoleType roleType) {
        Role role = new Role();
        role.setName(roleType);
        return role;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
            CustomUserDetailsService.class, UserController.class, UserService.class})
    static class TestWebConfiguration {
    }
}
