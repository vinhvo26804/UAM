package com.example.demo.config;

import com.example.demo.controller.AuthController;
import com.example.demo.controller.RoleController;
import com.example.demo.controller.UserController;
import com.example.demo.respository.UserRepository;
import com.example.demo.service.AuthService;
import com.example.demo.service.JwtService;
import com.example.demo.service.RoleService;
import com.example.demo.service.UserService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OpenApiIntegrationTests.TestWebConfiguration.class, properties = {
        "jwt.secret=" + SecurityIntegrationTests.TEST_SECRET,
        "jwt.expiration=360000",
        "jwt.refresh-expiration=1296000000"
})
class OpenApiIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilterChain;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RoleService roleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(securityFilterChain)
                .build();
    }

    @Test
    void generatedDocumentIsPublicAndDescribesJwtAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.info.title").value("UAM API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/test'].get").exists())
                .andExpect(jsonPath("$.paths['/test'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/changePassword'].put.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/me'].get").exists())
                .andExpect(jsonPath("$.paths['/me'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/roles'].get").exists())
                .andExpect(jsonPath("$.paths['/api/roles'].get.security").doesNotExist());
    }

    @Test
    void publicAuthenticationOperationsOverrideTheGlobalJwtRequirement() throws Exception {
        var result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        for (String path : new String[]{"/loginWithJwt", "/refreshToken", "/logout",
                "/api/v1/auth/register", "/api/v1/auth/verify"}) {
            result.andExpect(jsonPath("$.paths['" + path + "'].post.security").isArray())
                    .andExpect(jsonPath("$.paths['" + path + "'].post.security").isEmpty());
        }
    }

    @Test
    void swaggerUiAndItsResourcesLoadWithoutAnAccessToken() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Swagger UI")));

        mockMvc.perform(get("/swagger-ui/swagger-ui.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"));

        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }

    @Test
    void documentationIgnoresAnInvalidInheritedBearerToken() throws Exception {
        for (String path : new String[]{"/v3/api-docs", "/v3/api-docs/swagger-config",
                "/v3/api-docs.yaml", "/swagger-ui/index.html"}) {
            mockMvc.perform(get(path).header("Authorization", "Bearer invalid.old.token"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void publishingDocumentationDoesNotMakeTheApiPublic() throws Exception {
        mockMvc.perform(get("/test"))
                .andExpect(status().isUnauthorized());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({OpenApiConfig.class, SecurityConfig.class, JwtAuthenticationFilter.class,
            JwtService.class, AuthController.class, UserController.class, RoleController.class})
    static class TestWebConfiguration {
    }
}
