package com.taskcenter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        String renderUrl = System.getenv("RENDER_EXTERNAL_URL");
        if (renderUrl == null || renderUrl.isBlank()) {
            renderUrl = "https://task-center-backend.onrender.com";
        }
        return new OpenAPI()
            .components(new Components().addSecuritySchemes("bearerAuth",
                    new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .addServersItem(new Server().url("http://localhost:8080").description("Local - localhost:8080"))
            .addServersItem(new Server().url(renderUrl).description("Render - production"));
    }
}