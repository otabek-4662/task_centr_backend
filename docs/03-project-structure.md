# 03. Loyiha strukturasi

Yuqori darajadagi real tuzilma (Maven loyiha, `com.taskcenter:backend`):

```
.
├── pom.xml                      # Spring Boot 3.2.5 parent, Java 17
├── mvnw / mvnw.cmd / .mvn/      # Maven wrapper
├── Dockerfile                   # multi-stage build → JRE-alpine runtime
├── docker-compose.yml           # postgres:16 + backend (lokal dev)
├── render.yaml                  # Render blueprint (branch: master)
├── start.bat                    # Windows shortcut: mvnw spring-boot:run
├── swagger_smoke_test.ps1       # live-API smoke skript (~39 tekshiruv)
├── load/k6/                     # k6 load skriptlar (load.js, smoke.js, ratelimit-probe.js)
├── db/                          # yordamchi SQL (least-privilege user)
└── src/
    ├── main/java/com/taskcenter/
    │   ├── BackendApplication.java
    │   ├── controller/   # Auth, Workspace, Board, User controllerlar + GlobalExceptionHandler
    │   ├── service/      # Auth, Workspace, Board, Authorization, WebSocketEventPublisher
    │   ├── repository/   # User, Workspace, WorkspaceMember, Column, Task, Label
    │   ├── model/        # User (+Role), Workspace, BoardColumn, Task, Label,
    │   │                 #   WorkspaceMember(+Id), WorkspaceRole, Priority
    │   ├── dto/          # *Request, *Dto, AuthResponse, ApiResponse, BoardEvent, UserStatsDto
    │   ├── security/     # JwtTokenProvider, JwtAuthenticationFilter, RateLimitFilter, CustomUserDetailsService
    │   ├── config/       # Security, WebSocket(+2 interceptor), Cache, RateLimit,
    │   │                 #   Swagger, DataSeeder, AuditorAware
    │   └── monitoring/   # DatabaseHealthIndicator, RequestLoggingFilter
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/ # V1..V10 Flyway migratsiyalar
    └── test/
        ├── java/com/taskcenter/
        │   ├── controller/ # Auth, Board, User, Workspace MockMvc testlar
        │   ├── service/    # Auth, Board, Workspace, Authorization, Publisher, KeyPrefix unit testlar
        │   ├── repository/ # WorkspaceQueriesTest
        │   └── security/   # JwtTokenProviderTest, SecurityTest
        └── resources/application-test.yml  # H2, Flyway o'chiq, rate limit o'chiq
```

## Asosiy sinflar qanday bog'langan

- `BoardController` `BoardService` ni chaqiradi, u esa column/task/label/member/user/workspace
  repository'lari, `WorkspaceAuthorizationService` va `WebSocketEventPublisher` bilan
  ishlaydi. Eng keng servis shu: column'lar, board view, tasklar, labellar, a'zolar.
- `WorkspaceController` `WorkspaceService` ni chaqiradi (workspace/member/user
  repository'lari). Key-prefix generatsiya (`resolveUniqueKeyPrefix`) va
  owner-membership bootstrap shu yerda.
- `AuthController` `AuthService` ni chaqiradi (`AuthenticationManager`,
  `UserRepository`, `PasswordEncoder`, `JwtTokenProvider`).
- `UserController` repository'lar bilan to'g'ridan-to'g'ri ishlaydi: `/api/me`,
  `/api/me/stats`, `/api/users`.
- `WorkspaceAuthorizationService` workspace'ga tegishli har bir operatsiyadan oldin
  ikkala servis tomonidan ham chaqiriladi.
- `WebSocketEventPublisher` muvaffaqiyatli yozuvlardan keyin `BoardService` tomonidan
  chaqirilib, `/topic/board/{workspaceId}` ga `BoardEvent` yuboradi.
