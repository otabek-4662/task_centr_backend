package com.taskcenter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI(@Value("${spring.profiles.active:local}") String activeProfile) {
        String renderUrl = System.getenv("RENDER_EXTERNAL_URL");
        if (renderUrl == null || renderUrl.isBlank()) {
            renderUrl = "https://task-center-backend.onrender.com";
        }
        return new OpenAPI()
            .info(new Info().title("Task Center API").description("1. POST /api/auth/register yoki POST /api/auth/login dan AuthResponse.token oling (7 kun amal qiladi). 2. Yuqoridagi Authorize \uD83D\uDD13 tugmasini bosing, token ni qo'ying (Swagger Bearer prefiksini o'zi qo'shadi). 3. Qolgan endpointlarni chaqiring. Tokensiz faqat login/register ishlaydi.").version("1.0"))
            .components(new Components().addSecuritySchemes("bearerAuth",
                    new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT").in(SecurityScheme.In.HEADER).name("Authorization").description("JWT from POST /api/auth/login -> response.data.token. Paste token only, Swagger adds 'Bearer ' prefix.")))
            .addServersItem(new Server().url("http://localhost:8080").description("Local - localhost:8080"))
            .addServersItem(new Server().url("https://task-centr-backend.onrender.com").description("Render - production"));
    }

    @Bean
    public org.springdoc.core.models.GroupedOpenApi allOpenApi() {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("0-all")
                .displayName("0. Barchasi (All APIs)")
                .pathsToMatch("/**")
                .build();
    }

    @Bean
    public org.springdoc.core.models.GroupedOpenApi authOpenApi() {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("1-auth-users")
                .displayName("1. Autentifikatsiya va Profil (Auth & Users)")
                .addOpenApiMethodFilter(method ->
                        method.getDeclaringClass().equals(com.taskcenter.controller.AuthController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.UserController.class))
                .build();
    }

    @Bean
    public org.springdoc.core.models.GroupedOpenApi workspaceOpenApi() {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("2-workspaces")
                .displayName("2. Ishchi maydonlar (Workspaces & Members)")
                .addOpenApiMethodFilter(method ->
                        method.getDeclaringClass().equals(com.taskcenter.controller.WorkspaceController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.WorkspaceMemberController.class))
                .build();
    }

    @Bean
    public org.springdoc.core.models.GroupedOpenApi taskBoardOpenApi() {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("3-tasks-board")
                .displayName("3. Tasks & Kanban Board")
                .addOpenApiMethodFilter(method ->
                        method.getDeclaringClass().equals(com.taskcenter.controller.BoardController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.ColumnController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.TaskController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.CommentController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.AttachmentController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.TaskActivityController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.LabelController.class))
                .build();
    }

    @Bean
    public org.springdoc.core.models.GroupedOpenApi sprintOpenApi() {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("4-sprints")
                .displayName("4. Sprints (Agile / Scrum)")
                .addOpenApiMethodFilter(method ->
                        method.getDeclaringClass().equals(com.taskcenter.controller.SprintController.class))
                .build();
    }
}