package com.library.lms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI descriptor. Serves Swagger UI at {@code /swagger-ui.html} and the raw
 * spec at {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI libraryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Library Management System API")
                        .version("1.0")
                        .description("""
                                Dual-role library system.

                                **Authenticating:** POST to `/api/auth/login`, then click
                                *Authorize* above and paste the token. Every `/api/admin/**`
                                and `/api/user/**` endpoint needs it.

                                **Errors** all share one shape: `{ error, code, status, timestamp }`,
                                with an extra `fields` map on validation failures.
                                """)
                        .contact(new Contact().name("Daksh").email("dakshshah692@gmail.com"))
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the token from /api/auth/login")));
    }
}
