package com.example.demo.controller;

import com.example.demo.config.JwtAuthenticationFilter;
import com.example.demo.config.SecurityConfig;
import com.example.demo.dto.request.ResetPasswordRequest;
import com.example.demo.respository.UserRepository;
import com.example.demo.service.JwtService;
import com.example.demo.service.PasswordResetService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = PasswordResetControllerTests.TestWebConfiguration.class)
class PasswordResetControllerTests {

    private static final String FORGOT_PASSWORD = "/api/v1/auth/forgot-password";
    private static final String RESET_PASSWORD = "/api/v1/auth/reset-password";
    private static final String VALID_RESET = """
            {"userName":"alice","otpCode":"123456","newPassword":"new-password"}
            """;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilterChain;

    @MockitoBean
    private PasswordResetService service;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(service, userRepository, userDetailsService, jwtService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(securityFilterChain)
                .build();
    }

    @Test
    void otpRequestIsPublicAndDoesNotReturnTheCode() throws Exception {
        mockMvc.perform(post(FORGOT_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"alice"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.otpCode").doesNotExist())
                .andExpect(jsonPath("$.otp").doesNotExist());

        verify(service).requestOtp(argThat(request -> "alice".equals(request.getUserName())));
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void validResetIsPublicAndReturnsNoContent() throws Exception {
        mockMvc.perform(post(RESET_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RESET))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).resetPassword(argThat(request ->
                "alice".equals(request.getUserName())
                        && "123456".equals(request.getOtpCode())
                        && "new-password".equals(request.getNewPassword())));
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void inheritedInvalidBearerDoesNotBlockEitherRecoveryEndpoint() throws Exception {
        mockMvc.perform(post(FORGOT_PASSWORD)
                        .header("Authorization", "Bearer invalid.old.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"alice"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post(RESET_PASSWORD)
                        .header("Authorization", "Bearer invalid.old.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RESET))
                .andExpect(status().isNoContent());

        verify(service).requestOtp(any());
        verify(service).resetPassword(any());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void requestRejectsMissingNullAndBlankUserNames() throws Exception {
        for (String body : new String[]{"{}", "{\"userName\":null}", "{\"userName\":\"   \"}",
                "{\"userName\":\"" + "a".repeat(51) + "\"}"}) {
            mockMvc.perform(post(FORGOT_PASSWORD)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void resetRejectsMissingFieldsMalformedCodesAndShortPasswords() throws Exception {
        String[] invalidBodies = {
                "{}",
                "{\"otpCode\":\"123456\",\"newPassword\":\"new-password\"}",
                "{\"userName\":\"   \",\"otpCode\":\"123456\",\"newPassword\":\"new-password\"}",
                "{\"userName\":\"alice\",\"newPassword\":\"new-password\"}",
                "{\"userName\":\"alice\",\"otpCode\":null,\"newPassword\":\"new-password\"}",
                "{\"userName\":\"alice\",\"otpCode\":\"12345\",\"newPassword\":\"new-password\"}",
                "{\"userName\":\"alice\",\"otpCode\":\"1234567\",\"newPassword\":\"new-password\"}",
                "{\"userName\":\"alice\",\"otpCode\":\"abcdef\",\"newPassword\":\"new-password\"}",
                "{\"userName\":\"alice\",\"otpCode\":\"123456\"}",
                "{\"userName\":\"alice\",\"otpCode\":\"123456\",\"newPassword\":null}",
                "{\"userName\":\"alice\",\"otpCode\":\"123456\",\"newPassword\":\"        \"}",
                "{\"userName\":\"alice\",\"otpCode\":\"123456\",\"newPassword\":\"short\"}",
                "{\"userName\":\"alice\",\"otpCode\":\"123456\",\"newPassword\":\"" + "a".repeat(73) + "\"}"
        };
        for (String body : invalidBodies) {
            mockMvc.perform(post(RESET_PASSWORD)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void bothEndpointsRequireJsonBodyAndPostMethod() throws Exception {
        for (String endpoint : new String[]{FORGOT_PASSWORD, RESET_PASSWORD}) {
            mockMvc.perform(post(endpoint).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(post(endpoint).contentType(MediaType.APPLICATION_JSON).content("{bad-json"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(get(endpoint)).andExpect(status().isMethodNotAllowed());
        }
        verifyNoInteractions(service);
    }

    @Test
    void invalidOtpReturnsBadRequestWithGenericJsonError() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP"))
                .when(service).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post(RESET_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RESET))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid or expired OTP"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, PasswordResetController.class})
    static class TestWebConfiguration {
    }
}
