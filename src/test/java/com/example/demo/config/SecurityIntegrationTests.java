package com.example.demo.config;

import com.example.demo.controller.AuthController;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.RefreshTokenRequest;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.respository.UserRepository;
import com.example.demo.service.AuthService;
import com.example.demo.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = SecurityIntegrationTests.TestWebConfiguration.class)
@TestPropertySource(properties = {
        "jwt.secret=" + SecurityIntegrationTests.TEST_SECRET,
        "jwt.expiration=360000",
        "jwt.refresh-expiration=1296000000"
})
class SecurityIntegrationTests {

    static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWYwMTIzNDU2Nzg5YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZg==";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilterChain;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(userDetailsService, authService);
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(User.withUsername("alice")
                        .password("unused")
                        .roles("USER")
                        .build());
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(securityFilterChain)
                .build();
    }

    @Test
    void missingAuthorizationReturnsUnauthorizedWithExplanation() throws Exception {
        mockMvc.perform(get("/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", containsString("Authentication required")));
    }

    @Test
    void malformedAccessTokenReturnsUnauthorizedWithExplanation() throws Exception {
        mockMvc.perform(get("/test").header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", containsString("Invalid access token")));
    }

    @Test
    void expiredAccessTokenReturnsUnauthorizedWithExplanation() throws Exception {
        mockMvc.perform(get("/test").header("Authorization", "Bearer " + expiredAccessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", containsString("expired")));
    }

    @Test
    void validAccessTokenReachesProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/test")
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken("alice")))
                .andExpect(status().isOk())
                .andExpect(content().string(" Access success"));
    }

    @Test
    void bearerSchemeIsCaseInsensitiveAndTokenWhitespaceIsTrimmed() throws Exception {
        mockMvc.perform(get("/test")
                        .header("Authorization", "bearer   " + jwtService.generateAccessToken("alice") + " "))
                .andExpect(status().isOk())
                .andExpect(content().string(" Access success"));
    }

    @Test
    void refreshTokenCannotAuthenticateProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/test")
                        .header("Authorization", "Bearer " + jwtService.generateRefreshToken("alice")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", containsString("Access token required")));
    }

    @Test
    void unknownUserReturnsUnauthorizedWithExplanation() throws Exception {
        when(userDetailsService.loadUserByUsername("removed-user"))
                .thenThrow(new UsernameNotFoundException("User no longer exists"));

        mockMvc.perform(get("/test")
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken("removed-user")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void emptyBearerTokenReturnsUnauthorizedWithExplanation() throws Exception {
        mockMvc.perform(get("/test").header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void loginWithoutAuthorizationReachesAuthService() throws Exception {
        when(authService.loginWithJwt(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/loginWithJwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"alice","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

        verify(authService).loginWithJwt(argThat(request ->
                "alice".equals(request.getUserName()) && "test-password".equals(request.getPassword())));
    }

    @Test
    void loginIgnoresInvalidBearerTokenInheritedFromClient() throws Exception {
        when(authService.loginWithJwt(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/loginWithJwt")
                        .header("Authorization", "Bearer invalid.old.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"alice","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

        verify(authService).loginWithJwt(argThat(request ->
                "alice".equals(request.getUserName()) && "test-password".equals(request.getPassword())));
    }

    @Test
    void refreshWithExpiredBearerPassesJsonRefreshTokenToAuthService() throws Exception {
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenReturn(new LoginResponse("renewed-access-token", "stored-refresh-token"));

        mockMvc.perform(post("/refreshToken")
                        .header("Authorization", "Bearer " + expiredAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"stored-refresh-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("renewed-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("stored-refresh-token"));

        verify(authService).refreshToken(argThat(request ->
                "stored-refresh-token".equals(request.getRefreshToken())));
    }

    @Test
    void logoutWithoutAccessTokenReachesControllerAndReturnsNoContent() throws Exception {
        mockMvc.perform(post("/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"stored-refresh-token"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(authService).logout(argThat(request ->
                "stored-refresh-token".equals(request.getRefreshToken())));
    }

    @Test
    void logoutWithExpiredAccessTokenStillRevokesSession() throws Exception {
        mockMvc.perform(post("/logout")
                        .header("Authorization", "Bearer " + expiredAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"stored-refresh-token"}
                                """))
                .andExpect(status().isNoContent());

        verify(authService).logout(argThat(request ->
                "stored-refresh-token".equals(request.getRefreshToken())));
    }

    @Test
    void logoutWithMalformedInheritedBearerStillReachesService() throws Exception {
        mockMvc.perform(post("/logout")
                        .header("Authorization", "Bearer invalid.old.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"unknown-token"}
                                """))
                .andExpect(status().isNoContent());

        verify(authService).logout(argThat(request ->
                "unknown-token".equals(request.getRefreshToken())));
    }

    @Test
    void getLogoutCannotRevokeSession() throws Exception {
        mockMvc.perform(get("/logout")
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken("alice")))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(authService);
    }

    @Test
    void logoutAndRefreshRejectMissingBlankAndOversizedTokens() throws Exception {
        for (String endpoint : new String[]{"/logout", "/refreshToken"}) {
            for (String body : new String[]{"{}", "{\"refreshToken\":null}", "{\"refreshToken\":\"   \"}",
                    "{\"refreshToken\":\"" + "a".repeat(4097) + "\"}"}) {
                mockMvc.perform(post(endpoint)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isBadRequest());
            }
            mockMvc.perform(post(endpoint).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(authService);
    }

    @Test
    void rejectedRefreshTokenReturnsUnauthorizedJsonInsteadOfServerError() throws Exception {
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or revoked refresh token"));

        mockMvc.perform(post("/refreshToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"revoked-refresh-token"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid or revoked refresh token"));
    }

    private String expiredAccessToken() {
        return Jwts.builder()
                .subject("alice")
                .claim("type", "access")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .compact();
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, AuthController.class})
    static class TestWebConfiguration {
    }
}
