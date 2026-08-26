package com.taskcenter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
            .info(new Info().title("Task Center API").description("1. POST /api/auth/register yoki POST /api/auth/login dan AuthResponse.token oling (7 kun amal qiladi). 2. Yuqoridagi Authorize \uD83D\uDD13 tugmasini bosing, token ni qo'ying (Swagger Bearer prefiksini o'zi qo'shadi). 3. Qolgan endpointlarni chaqiring. Tokensiz faqat login/register ishlaydi.").version("1.0"))
            .components(new Components().addSecuritySchemes("bearerAuth",
                    new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT").in(SecurityScheme.In.HEADER).name("Authorization").description("JWT from POST /api/auth/login -> response.data.token. Paste token only, Swagger adds 'Bearer ' prefix.")))
            .addServersItem(new Server().url("http://localhost:8080").description("Local - localhost:8080"))
            .addServersItem(new Server().url(renderUrl).description("Render - production"));
    }
}