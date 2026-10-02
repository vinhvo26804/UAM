package com.example.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI uamOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UAM API")
                        .version("v1")
                        .description("User authentication, profile and role management APIs."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Log in with /loginWithJwt, then paste the accessToken here without the Bearer prefix.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
