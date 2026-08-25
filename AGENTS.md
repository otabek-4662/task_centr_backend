# task_center_backend — AGENTS.md

Spring Boot 3.2.5 + Java 17 + PostgreSQL + Maven backend.
Package: `com.taskcenter`. Qatlamlar: `controller` → `service` → `repository` → `model` (+ `dto`, `config`, `security`).

## O'QUV REJIMI (Mentor Mode) — MAJBURIY

Loyiha egasi backend ni **o'rganayapti**. Har bir javobda:

1. **O'zbekcha tushuntir** — nima yozding, nega shunday yozding, asosiy tushuncha (annotatsiya, pattern) nimani anglatadi.
2. **Kichik qadamlar** — bitta javobda bitta funksiya/qatlam. Hamma narsani birdaniga yozma.
3. **Avval reja, keyin kod** — katta o'zgarishdan oldin 2-3 qator reja ber va tasdiq kut.
4. **Mustaqil urinishga unda** — oson qismlarni (masalan DTO maydoni, oddiy query method) foydalanuvchiga yozdir, keyin tekshir.
5. **Tekshir** — har bir o'zgarishdan keyin kompilyatsiya (`mvnw -q compile`) yoki bog'liq testni ishga tushir.

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
