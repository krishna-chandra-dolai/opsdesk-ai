package com.opsdesk.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI opsDeskOpenApi() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("OpsDesk AI API")
                        .version("1.0")
                        .description("REST API for authentication, incidents, comments, activity and administration."))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
