# task_center_backend — AGENTS.md

Spring Boot 3.2.5 + Java 17 + PostgreSQL + Maven backend.
Package: `com.taskcenter`. Qatlamlar: `controller` → `service` → `repository` → `model` (+ `dto`, `config`, `security`).

## Loyiha konvensiyalari

- Constructor injection (`@Autowired` field larda yo'q)
- API javoblari `ApiResponse.success("ok", data)` wrapper da
- Ro'yxatlar `Pageable` bilan (pagination majburiy)
- Validatsiya: `@Valid @RequestBody` har bir mutating endpoint da
- Xatolar: `@RestControllerAdvice` orqali, stack trace siz
- N+1 dan saqlan: `EntityGraph` / `JOIN FETCH`
- Secret lar faqat env variable da — `application.yml` ga yozma

## Buyruqlar

- Build: `mvnw -q compile` (Windows: `mvnw.cmd`)
- Testlar: `mvnw test`
- PostgreSQL: `docker-compose.yml` orqali
- Swagger: `/swagger-ui.html`

## Cheklovlar

- Git commit/push faqat so'ralganda
- `application.yml` dagi mavjud sozlamalarni sababsiz o'zgartirma
- `target/`, `*.log`, `app.jar` ga tegma
