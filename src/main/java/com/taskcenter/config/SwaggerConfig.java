package com.taskcenter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI(@Value("${spring.profiles.active:local}") String activeProfile) {
        String renderUrl = System.getenv("RENDER_EXTERNAL_URL");
        if (renderUrl == null || renderUrl.isBlank()) {
            renderUrl = "https://task-centr-backend.onrender.com";
        }

        Schema<?> errorResponseSchema = new Schema<>()
                .type("object")
                .description("Standart xatolik javobi formati")
                .addProperty("success", new BooleanSchema().example(false).description("Doimiy false"))
                .addProperty("message", new StringSchema().example("Xatolik tavsifi").description("Foydalanuvchiga tushunarli xabar"))
                .addProperty("data", new ObjectSchema().description("Validatsiya xatolarida xatolar xaritasi {maydon: xabar}, aks holda null"))
                .addProperty("code", new StringSchema().example("BAD_REQUEST").description("Xatolik kodi (backward compatibility)"))
                .addProperty("errorCode", new StringSchema().example("INVITE_NOT_FOUND").description("Xatolikning mashina o'qiy oladigan enum kodi"))
                .addProperty("fieldErrors", new ArraySchema().description("Validatsiya xatolarida maydon nomlari va tafsilotlari ro'yxati"))
                .addProperty("retryAfterSeconds", new IntegerSchema().example(45).description("Rate limit (429) bo'lganda kutish vaqti soniyalarda"))
                .addProperty("status", new IntegerSchema().example(400).description("HTTP status kodi"))
                .addProperty("timestamp", new DateTimeSchema().example("2026-10-08T17:00:00").description("Xatolik yuz bergan vaqt (ISO-8601 UTC)"));

        Schema<?> pageResponseSchema = createPageResponseSchema();

        String apiDescription = "### Task Center Backend REST API\n\n" +
                "Loyiha boshqaruvi, Kanban doskalari, vazifalar, sprintlar va jamoaviy chat platformasi.\n\n" +
                "#### 🔐 Autentifikatsiya (JWT):\n" +
                "1. `POST /api/auth/register` yoki `POST /api/auth/login` orqali tizimga kiring va `token` oling (token 7 kun amal qiladi).\n" +
                "2. Yuqoridagi **Authorize 🔓** tugmasini bosing va tokenni kiriting (Swagger `Bearer ` prefiksini avtomatik qo'shadi).\n" +
                "3. Token muddati o'tganda `POST /api/auth/refresh` orqali yangilang.\n\n" +
                "#### 📦 Standart Javob Wrappper (`ApiResponse<T>`):\n" +
                "- Barcha muvaffaqiyatli javoblar `{ success: true, message: 'ok', data: ... }` ko'rinishida qaytadi.\n" +
                "- Ro'yxatlar uchun pagination Spring `Pageable` parametri orqali (`page=0&size=20&sort=createdAt,desc`) uzatiladi.\n\n" +
                "#### ⚠️ Xatoliklar:\n" +
                "- 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict, 500 Server Error.\n" +
                "- Validatsiya xatolarida `data` maydonida `{ \"field\": \"xato xabari\" }` obyekti qaytadi.";

        return new OpenAPI()
                .info(new Info()
                        .title("Task Center API")
                        .description(apiDescription)
                        .version("2.0.0")
                        .contact(new io.swagger.v3.oas.models.info.Contact()
                                .name("Task Center Team")
                                .email("dev@taskcenter.com")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")
                                        .description("JWT access token. Faqat tokenni o'zini kiriting, 'Bearer ' prefiksi avtomatik qo'shiladi."))
                        .addSchemas("ErrorResponse", errorResponseSchema)
                        .addSchemas("PageResponse", pageResponseSchema))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Auth").description("Autentifikatsiya va avtorizatsiya (login, register, token refresh, parol tiklash)"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Telegram Auth").description("Telegram Mini App autentifikatsiyasi"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Users").description("Foydalanuvchi profili, shaxsiy vazifalar va Telegram integratsiyasi"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Workspaces").description("Ishchi maydonlar (Workspace CRUD)"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Workspace Members").description("Ishchi maydon a'zolari va rollarini boshqarish"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Workspace Invitations").description("Ishchi maydonga taklifnomalar yuborish va qabul qilish"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Board").description("Kanban doskasi ma'lumotlari (barcha ustunlar va vazifalar)"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Columns").description("Ustunlar (statuslar) CRUD va qayta tartiblash"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Directions").description("Yo'nalishlar (Frontend, Backend, QA, Mobile va h.k.)"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Labels").description("Teglar (Labels) boshqaruvi"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Tasks").description("Vazifalar (Tasks) CRUD, saralash va boshqarish"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Checklists").description("Vazifa quyi topshiriqlari (Checklist elementlari)"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Comments").description("Vazifaga izohlar qoldirish va boshqarish"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Attachments").description("Vazifaga fayl va rasm biriktirish"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Activities").description("Vazifaning o'zgarishlar tarixi (Audit log)"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Sprints").description("Agile & Scrum Sprint boshqaruvi"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Reports").description("Loyiha va Sprint hisobotlari (Summary, Burn-down)"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Export").description("Vazifalar ma'lumotlarini CSV formatida eksport qilish"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Notifications").description("Foydalanuvchi bildirishnomalari"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Chat").description("Umumiy va shaxsiy chat xabarlari"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Fayllar (Files)").description("Fayllarni umumiy yuklash va yuklab olish"))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Konfiguratsiya (Config)").description("Tizimning umumiy konfiguratsiyasi"))
                .addServersItem(new Server().url("http://localhost:8080").description("Lokal ishlab chiqish muhiti (Local: localhost:8080)"))
                .addServersItem(new Server().url(renderUrl).description("Jonli server (Production)"));
    }

    @Bean
    public OpenApiCustomizer globalErrorResponsesCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }

            if (openApi.getComponents().getSchemas() == null || !openApi.getComponents().getSchemas().containsKey("ErrorResponse")) {
                Schema<?> errorResponseSchema = new Schema<>()
                        .type("object")
                        .description("Standart xatolik javobi formati")
                        .addProperty("success", new BooleanSchema().example(false).description("Doimiy false"))
                        .addProperty("message", new StringSchema().example("Xatolik tavsifi").description("Foydalanuvchiga tushunarli xabar"))
                        .addProperty("data", new ObjectSchema().description("Validatsiya xatolarida xatolar xaritasi {maydon: xabar}, aks holda null"))
                        .addProperty("code", new StringSchema().example("BAD_REQUEST").description("Xatolik kodi (backward compatibility)"))
                        .addProperty("errorCode", new StringSchema().example("INVITE_NOT_FOUND").description("Xatolikning mashina o'qiy oladigan enum kodi"))
                        .addProperty("fieldErrors", new ArraySchema().description("Validatsiya xatolarida maydon nomlari va tafsilotlari ro'yxati"))
                        .addProperty("retryAfterSeconds", new IntegerSchema().example(45).description("Rate limit (429) bo'lganda kutish vaqti soniyalarda"))
                        .addProperty("status", new IntegerSchema().example(400).description("HTTP status kodi"))
                        .addProperty("timestamp", new DateTimeSchema().example("2026-10-08T17:00:00").description("Xatolik yuz bergan vaqt (ISO-8601 UTC)"));
                openApi.getComponents().addSchemas("ErrorResponse", errorResponseSchema);
            }

            if (openApi.getComponents().getSchemas() == null || !openApi.getComponents().getSchemas().containsKey("PageResponse")) {
                openApi.getComponents().addSchemas("PageResponse", createPageResponseSchema());
            }

            if (openApi.getComponents().getSecuritySchemes() == null || !openApi.getComponents().getSecuritySchemes().containsKey("bearerAuth")) {
                openApi.getComponents().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("JWT access token. Faqat tokenni o'zini kiriting, 'Bearer ' prefiksi avtomatik qo'shiladi."));
            }

            Content errorContent = new Content().addMediaType(
                    org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
            );

            ApiResponse badRequest = new ApiResponse().description("Noto'g'ri so'rov yoki validatsiya xatosi (400 Bad Request)").content(errorContent);
            ApiResponse unauthorized = new ApiResponse().description("Autentifikatsiya xatosi yoki token yaroqsiz (401 Unauthorized)").content(errorContent);
            ApiResponse forbidden = new ApiResponse().description("Amalni bajarish uchun ruxsat yetarli emas (403 Forbidden)").content(errorContent);
            ApiResponse notFound = new ApiResponse().description("Resurs topilmadi (404 Not Found)").content(errorContent);
            ApiResponse conflict = new ApiResponse().description("Ma'lumotlar to'qnashuvi yoki dublikat (409 Conflict)").content(errorContent);
            ApiResponse rateLimited = new ApiResponse().description("So'rovlar soni limiti oshdi (429 Too Many Requests)").content(errorContent);
            ApiResponse serverError = new ApiResponse().description("Server ichki xatoligi (500 Internal Server Error)").content(errorContent);

            if (openApi.getPaths() != null) {
                openApi.getPaths().values().forEach(pathItem -> {
                    pathItem.readOperations().forEach(operation -> {
                        ApiResponses responses = operation.getResponses();
                        if (responses != null) {
                            if (!responses.containsKey("400")) responses.addApiResponse("400", badRequest);
                            if (!responses.containsKey("401")) responses.addApiResponse("401", unauthorized);
                            if (!responses.containsKey("403")) responses.addApiResponse("403", forbidden);
                            if (!responses.containsKey("404")) responses.addApiResponse("404", notFound);
                            if (!responses.containsKey("409")) responses.addApiResponse("409", conflict);
                            if (!responses.containsKey("429")) responses.addApiResponse("429", rateLimited);
                            if (!responses.containsKey("500")) responses.addApiResponse("500", serverError);
                        }

                        if (operation.getParameters() != null) {
                            for (var param : operation.getParameters()) {
                                if ("page".equals(param.getName())) {
                                    param.setDescription("Sahifa indeksi (0 dan boshlanadi)");
                                    param.setExample(0);
                                } else if ("size".equals(param.getName())) {
                                    param.setDescription("Sahifadagi elementlar soni (standart: 20)");
                                    param.setExample(20);
                                } else if ("sort".equals(param.getName())) {
                                    param.setDescription("Saralash sharti: maydon,(asc|desc)");
                                    param.setExample(java.util.List.of("createdAt,desc"));
                                } else if ("search".equals(param.getName())) {
                                    if (param.getDescription() == null || param.getDescription().isBlank()) {
                                        param.setDescription("Qidiruv so'zi (nom yoki tavsif bo'yicha)");
                                    }
                                    if (param.getExample() == null) param.setExample("Swagger");
                                } else if ("status".equals(param.getName())) {
                                    if (param.getDescription() == null || param.getDescription().isBlank()) {
                                        param.setDescription("Holat bo'yicha filtrlash");
                                    }
                                    if (param.getExample() == null) param.setExample("ACTIVE");
                                } else if ("includeArchived".equals(param.getName())) {
                                    if (param.getDescription() == null || param.getDescription().isBlank()) {
                                        param.setDescription("Arxivlangan elementlarni ham qo'shish");
                                    }
                                    if (param.getExample() == null) param.setExample(false);
                                }
                            }
                        }
                    });
                });
            }
        };
    }

    private Schema<?> createPageResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("Standart sahifalangan (pagination) ma'lumotlar formati")
                .addProperty("totalElements", new IntegerSchema().format("int64").example(42).description("Jami elementlar soni"))
                .addProperty("totalPages", new IntegerSchema().format("int32").example(3).description("Jami sahifalar soni"))
                .addProperty("first", new BooleanSchema().example(true).description("Birinchi sahifami"))
                .addProperty("last", new BooleanSchema().example(false).description("Oxirgi sahifami"))
                .addProperty("size", new IntegerSchema().format("int32").example(20).description("Sahifa o'lchami"))
                .addProperty("content", new ArraySchema().description("Sahifadagi elementlar ro'yxati"))
                .addProperty("number", new IntegerSchema().format("int32").example(0).description("Joriy sahifa indeksi (0 dan boshlanadi)"))
                .addProperty("sort", new ArraySchema().items(new Schema<>().$ref("#/components/schemas/SortObject")).description("Saralash ma'lumotlari"))
                .addProperty("numberOfElements", new IntegerSchema().format("int32").example(20).description("Joriy sahifadagi elementlar soni"))
                .addProperty("pageable", new Schema<>().$ref("#/components/schemas/PageableObject").description("Sahifalash konfiguratsiyasi"))
                .addProperty("empty", new BooleanSchema().example(false).description("Sahifa bo'shmi"));
    }

    @Bean
    public org.springdoc.core.models.GroupedOpenApi allOpenApi(OpenApiCustomizer customizer) {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("0-all")
                .displayName("0. Barchasi (All APIs)")
                .pathsToMatch("/**")
                .addOpenApiCustomizer(customizer)
                .build();
    }

    @Bean
    public org.springdoc.core.models.GroupedOpenApi appOpenApi(OpenApiCustomizer customizer) {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("app")
                .displayName("App (Faol API lar)")
                .pathsToMatch("/**")
                .addOpenApiMethodFilter(method ->
                        !method.isAnnotationPresent(Deprecated.class) &&
                        !method.getDeclaringClass().isAnnotationPresent(Deprecated.class))
                .addOpenApiCustomizer(customizer)
                .build();
    }

    @Bean
    public org.springdoc.core.models.GroupedOpenApi legacyOpenApi(OpenApiCustomizer customizer) {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("legacy")
                .displayName("Legacy (Barcha API lar)")
                .pathsToMatch("/**")
                .addOpenApiCustomizer(customizer)
                .build();
    }

    @Bean
    public org.springdoc.core.models.GroupedOpenApi authOpenApi(OpenApiCustomizer customizer) {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("1-auth-users")
                .displayName("1. Autentifikatsiya va Profil (Auth & Users)")
                .addOpenApiMethodFilter(method ->
                        method.getDeclaringClass().equals(com.taskcenter.controller.AuthController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.AuthV1LegacyController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.UserController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.TelegramAuthController.class))
                .addOpenApiCustomizer(customizer)
                .build();
    }

    @Bean
    public org.springdoc.core.models.GroupedOpenApi workspaceOpenApi(OpenApiCustomizer customizer) {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("2-workspaces")
                .displayName("2. Ishchi maydonlar (Workspaces & Members)")
                .addOpenApiMethodFilter(method ->
                        method.getDeclaringClass().equals(com.taskcenter.controller.WorkspaceController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.WorkspaceMemberController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.WorkspaceInvitationController.class))
                .addOpenApiCustomizer(customizer)
                .build();
    }

    @Bean
    public org.springdoc.core.models.GroupedOpenApi taskBoardOpenApi(OpenApiCustomizer customizer) {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("3-tasks-board")
                .displayName("3. Tasks & Kanban Board")
                .addOpenApiMethodFilter(method ->
                        method.getDeclaringClass().equals(com.taskcenter.controller.BoardController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.ColumnController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.TaskController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.TaskControllerV2.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.TaskDirectController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.CommentController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.AttachmentController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.TaskActivityController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.TaskChecklistController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.TaskChecklistDirectController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.LabelController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.DirectionController.class))
                .addOpenApiCustomizer(customizer)
                .build();
    }

    @Bean
    public org.springdoc.core.models.GroupedOpenApi sprintOpenApi(OpenApiCustomizer customizer) {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("4-sprints")
                .displayName("4. Sprints (Agile / Scrum)")
                .addOpenApiMethodFilter(method ->
                        method.getDeclaringClass().equals(com.taskcenter.controller.SprintController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.ReportController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.ExportController.class))
                .addOpenApiCustomizer(customizer)
                .build();
    }

    @Bean
    public org.springdoc.core.models.GroupedOpenApi chatOpenApi(OpenApiCustomizer customizer) {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("5-chat-more")
                .displayName("5. Chat, Bildirishnomalar va Fayllar (Chat & Notifications)")
                .addOpenApiMethodFilter(method ->
                        method.getDeclaringClass().equals(com.taskcenter.controller.ChatController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.NotificationController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.FileController.class) ||
                        method.getDeclaringClass().equals(com.taskcenter.controller.ConfigController.class))
                .addOpenApiCustomizer(customizer)
                .build();
    }
}
