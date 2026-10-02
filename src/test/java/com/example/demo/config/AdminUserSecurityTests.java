package com.example.demo.config;

import com.example.demo.controller.AdminUserController;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.respository.RefreshTokenRepository;
import com.example.demo.respository.UserRepository;
import com.example.demo.service.AdminUserService;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.JwtService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AdminUserSecurityTests.TestWebConfiguration.class)
@TestPropertySource(properties = {
        "jwt.secret=" + SecurityIntegrationTests.TEST_SECRET,
        "jwt.expiration=360000",
        "jwt.refresh-expiration=1296000000"
})
class AdminUserSecurityTests {

    private static final String BASE_PATH = "/api/admin/users";
    private static final String VALID_UPDATE = """
            {"email":"updated@example.com","fullname":"Updated User",
             "doB":"2001-02-03","phone":"+84912345678"}
            """;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilterChain;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    private MockMvc mockMvc;
    private User principal;
    private User target;

    @BeforeEach
    void setUp() {
        principal = User.builder()
                .Id(1L)
                .username("admin")
                .password("admin-password-hash")
                .email("admin@example.com")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role(Role.RoleType.ADMIN)))
                .build();
        target = User.builder()
                .Id(2L)
                .username("member")
                .password("private-password-hash")
                .email("member@example.com")
                .fullname("Member")
                .doB(LocalDate.of(2000, 1, 1))
                .phone("0123456789")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role(Role.RoleType.USER)))
                .build();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(securityFilterChain)
                .build();
    }

    @Test
    void adminCanListAndViewUsersWithoutExposingCredentials() throws Exception {
        authenticate();
        when(userRepository.searchUsers(null, null, null, null)).thenReturn(List.of(target));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        mockMvc.perform(get(BASE_PATH).header("Authorization", accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].username").value("member"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].roles[0]").value("USER"))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].refreshToken").doesNotExist());

        mockMvc.perform(get(BASE_PATH + "/2").header("Authorization", accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.fullname").value("Member"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.failedLoginAttemps").doesNotExist());

        verify(userRepository).searchUsers(null, null, null, null);
        verify(userRepository).findById(2L);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void adminCanUpdateProfileAndEmailChangeRevokesRefreshTokens() throws Exception {
        authenticate();
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));

        mockMvc.perform(put(BASE_PATH + "/2")
                        .header("Authorization", accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("member"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.fullname").value("Updated User"))
                .andExpect(jsonPath("$.phone").value("+84912345678"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        assertThat(target.getDoB()).isEqualTo(LocalDate.of(2001, 2, 3));
        assertThat(target.getPassword()).isEqualTo("private-password-hash");
        verify(userRepository).save(target);
        verify(refreshTokenRepository).revokeByUserId(2L);
    }

    @Test
    void adminDeleteDisablesAccountAndRevokesTokensWithoutDeletingAnyRow() throws Exception {
        authenticate();
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));

        mockMvc.perform(delete(BASE_PATH + "/2").header("Authorization", accessToken()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(target.getStatus()).isEqualTo(UserStatus.DISABLED);
        assertThat(target.getId()).isEqualTo(2L);
        assertThat(target.getEmail()).isEqualTo("member@example.com");
        assertThat(target.getPassword()).isEqualTo("private-password-hash");
        verify(userRepository).findByUsername("admin");
        verify(userRepository).findByIdForUpdate(2L);
        verify(userRepository).save(target);
        verifyNoMoreInteractions(userRepository);
        verify(refreshTokenRepository).revokeByUserId(2L);
        verifyNoMoreInteractions(refreshTokenRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "lock, ACTIVE, LOCKED, true",
            "unlock, LOCKED, ACTIVE, false",
            "disable, ACTIVE, DISABLED, true",
            "activate, DISABLED, ACTIVE, false"
    })
    void adminCanChangeAccountStatus(String action, UserStatus before, UserStatus after,
                                     boolean revokesRefreshTokens) throws Exception {
        authenticate();
        target.setStatus(before);
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));

        mockMvc.perform(patch(BASE_PATH + "/2/" + action).header("Authorization", accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(after.name()))
                .andExpect(jsonPath("$.password").doesNotExist());

        assertThat(target.getStatus()).isEqualTo(after);
        verify(userRepository).save(target);
        if (revokesRefreshTokens) {
            verify(refreshTokenRepository).revokeByUserId(2L);
        } else {
            verifyNoInteractions(refreshTokenRepository);
        }
    }

    @ParameterizedTest(name = "{0} cannot call {1}")
    @MethodSource("nonAdminEndpoints")
    void userAndModeratorCannotCallAnyAdminEndpoint(Role.RoleType roleType, Endpoint endpoint)
            throws Exception {
        principal.setRoles(Set.of(role(roleType)));
        authenticate();

        mockMvc.perform(endpoint.requestBuilder().header("Authorization", accessToken()))
                .andExpect(status().isForbidden());

        verify(userRepository).findByUsername("admin");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(refreshTokenRepository);
    }

    @ParameterizedTest(name = "anonymous cannot call {0}")
    @MethodSource("endpoints")
    void everyAdminEndpointRequiresAuthentication(Endpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.requestBuilder())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userRepository, refreshTokenRepository);
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"PENDING", "LOCKED", "DISABLED"})
    void existingAccessTokenStopsWorkingWhenAdminBecomesInactive(UserStatus inactiveStatus)
            throws Exception {
        authenticate();
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        String authorization = accessToken();

        mockMvc.perform(get(BASE_PATH + "/2").header("Authorization", authorization))
                .andExpect(status().isOk());

        principal.setStatus(inactiveStatus);

        mockMvc.perform(get(BASE_PATH + "/2").header("Authorization", authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Account is not active")));

        verify(userRepository, times(2)).findByUsername("admin");
        verify(userRepository).findById(2L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(refreshTokenRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"email\":\"not-an-email\",\"fullname\":\"Member\"}",
            "{\"email\":\"member@example.com\",\"fullname\":\" \"}",
            "{\"email\":\"member@example.com\",\"fullname\":\"Member\",\"phone\":\"invalid\"}",
            "{\"email\":\"member@example.com\",\"fullname\":\"Member\",\"doB\":\"9999-01-01\"}",
            "{\"email\":\"member@example.com\",\"fullname\":\"Member\",\"doB\":\"invalid\"}"
    })
    void invalidProfileIsRejectedBeforeAnyMutation(String body) throws Exception {
        authenticate();

        mockMvc.perform(put(BASE_PATH + "/2")
                        .header("Authorization", accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(userRepository).findByUsername("admin");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void missingUserReturnsNotFoundWithoutSaving() throws Exception {
        authenticate();

        mockMvc.perform(get(BASE_PATH + "/404").header("Authorization", accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));

        verify(userRepository).findByUsername("admin");
        verify(userRepository).findById(404L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(refreshTokenRepository);
    }

    private void authenticate() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(principal));
    }

    private String accessToken() {
        return "Bearer " + jwtService.generateAccessToken("admin");
    }

    private static Role role(Role.RoleType type) {
        Role role = new Role();
        role.setName(type);
        return role;
    }

    private static Stream<Endpoint> endpoints() {
        return Stream.of(
                new Endpoint("GET", BASE_PATH),
                new Endpoint("GET", BASE_PATH + "/2"),
                new Endpoint("PUT", BASE_PATH + "/2"),
                new Endpoint("DELETE", BASE_PATH + "/2"),
                new Endpoint("PATCH", BASE_PATH + "/2/lock"),
                new Endpoint("PATCH", BASE_PATH + "/2/unlock"),
                new Endpoint("PATCH", BASE_PATH + "/2/disable"),
                new Endpoint("PATCH", BASE_PATH + "/2/activate"));
    }

    private static Stream<Arguments> nonAdminEndpoints() {
        return Stream.of(Role.RoleType.USER, Role.RoleType.MODERATOR)
                .flatMap(roleType -> endpoints().map(endpoint -> Arguments.of(roleType, endpoint)));
    }

    private record Endpoint(String method, String path) {
        MockHttpServletRequestBuilder requestBuilder() {
            MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), path);
            if ("PUT".equals(method)) {
                builder.contentType(MediaType.APPLICATION_JSON).content(VALID_UPDATE);
            }
            return builder;
        }

        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
            CustomUserDetailsService.class, AdminUserController.class, AdminUserService.class})
    static class TestWebConfiguration {
    }
}
