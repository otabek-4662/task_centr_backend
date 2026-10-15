# task_center_backend — AGENTS.md

Spring Boot 3.2.5 + Java 17 + PostgreSQL + Flyway + WebSocket (STOMP) + Telegram Bot & Mini App (`/app/**`) backend.
Asosiy paket: `com.taskcenter`.

---

## 0. Yangi Chatda Taxminlarsiz Ishlash Protokoli (Zero-Guessing & Context-First Rule)

Har safar yangi AI sessiyasida (Cursor, Claude Code, Gemini, Copilot, Windsurf) vazifa bajarishdan oldin agent **quyidagi 3 ta qadamni bajarishi shart**:
1. **`docs/PROJECT_MAP.md` ni o'qing:** Loyihadagi barcha 28 ta JPA Entity va DB ustunlari, 24 ta Controller endpointlari, 25 ta Service metodlari, 22 ta Repository interfeyslari hamda `V1`–`V62` Flyway migratsiyalari `docs/PROJECT_MAP.md` ichida to'liq ro'yxatga olingan.
2. **Hech qachon jadval ustuni yoki metod nomini taxmin qilmang:**
   - `tasks` jadvalida `status` ustuni **yo'q** — vazifa holati `column_id` (`board_columns.id`) va tugallanganligi `board_columns.is_done` orqali aniqlanadi.
   - `chat_messages` jadvalida `workspace_id` ustuni **yo'q** — xabarlar `PUBLIC` yoki `DIRECT` (`sender_id`, `recipient_id`) turiga ega.
   - `User` entity hech qachon WebSocket yoki REST javobida to'g'ridan-to'g'ri qaytarilmaydi — har doim `UserDto`, `ChatUserDto` yoki `TaskDto` ishlatiladi.
3. **Xaritani yangilab boring:** Agar yangi Controller, Service, Entity yoki Flyway migratsiyasi qo'shsangiz, ish yakunida `python scripts/generate-project-map.py` skriptini ishga tushirib `docs/PROJECT_MAP.md` ni yangilang.

---

## 1. Loyiha Arxitekturasi va Modullar

| Paket / Katalog | Javobgarlik doirasi | Asosiy fayllar / Eslatmalar |
| :--- | :--- | :--- |
| `config/` | Spring Security, CORS, CSP, Cache, Async, ShedLock, WebSocket, OpenAPI, Bot Leader Lock | `SecurityConfig`, `CacheConfig`, `TelegramBotConfig`, `WebSocketConfig`, `OpenApiConfig`, `SchedulerLockConfig` |
| `security/` | JWT, Telegram `initData` HMAC-SHA256, Rate Limiting, `@CurrentUser` | `JwtAuthenticationFilter` (`TelegramWebAppAuthFilter` dan **oldin** ishlaydi!), `JwtTokenProvider`, `TelegramInitDataValidator`, `RateLimitFilter` |
| `controller/` | REST API (`/api/**`), Legacy v1 (`/api/v1/**`), Webhooks (`/api/webhooks/**`), STOMP (`/app/**`) | Barcha javoblar `ApiResponse<T>` yoki xatolikda `ApiError` (`GlobalExceptionHandler`) formatida qaytadi |
| `service/` | Biznes mantiq, tranzaksiyalar, bildirishnomalar, kesh tozalash, GC cronlar | `TaskService`, `SprintService`, `ChatService`, `WorkspaceInvitationService`, `TelegramBotService`, `EmailService` |
| `service/storage/` | Fayl saqlash strategiyalari (`storage.type`: `local`, `db`, `s3`) | `LocalFileStorageService`, `DatabaseFileStorageService` (`stored_files`), `S3FileStorageService` |
| `repository/` | Spring Data JPA, `@EntityGraph`, JPQL va PostgreSQL native so'rovlar | `TaskRepository`, `ChatMessageRepository`, `WorkspaceInvitationRepository`, `TelegramReminderLogRepository` |
| `model/` | JPA Entity'lar, `@Version` (optimistic lock), `@SQLDelete` / `deleted_at` (soft-delete) | `Task`, `Workspace`, `BoardColumn`, `Sprint`, `User` (`@JsonIgnore` on `password`/`telegramLinkToken`), `ChatMessage`, `WorkspaceInvitation`, `EmailJob` |
| `util/` | LexoRank (Base-36), vaqt (`AppTime`), DB URL normalizatsiyasi, token xeshlash | `LexoRankUtil`, `LexoRank`, `AppTime` (Storage: `UTC`, User: `Asia/Tashkent`), `InvitationTokenUtil` |
| `resources/db/migration/` | Flyway migratsiyalari (`V1` .. `V62`) | Faqat yangi `V<N>__*.sql` qo'shiladi; mavjud migratsiyalar o'zgartirilmaydi |
| `resources/static/app/` | Telegram Mini App & Responsive Web UI ("Choylashamiz") | `index.html`, `app.js`, `ui-bind.js` (CSP-compliant), `style.css` |

---

## 2. Argon 4 Majburiy Qoidalari (Zero-Regression & Audit Rules)

Oldingi sessiyalarda yo'l qo'yilgan xatolar (ketma-ket CI yiqilishi, terminal osilib qolishi, zip ustiga yozish oqibatida prod qulashi) qaytarilmasligi uchun quyidagi **6 ta qat'iy qoida** bajarilishi shart:

### 2.1. "Push & Pray" Taqiqlanadi — Lokal Pre-Flight Tekshiruv (3 Bosqich)
Git commit yoki push qilishdan oldin `.github/workflows/ci.yml` dagi barcha 3 ta bosqich lokal muhitda to'liq tekshirilishi shart:
1. **H2 va Profile Testlar:**
   - Linux/macOS: `./mvnw -B test`
   - Windows: `mvnw.cmd -B test`
   - Profil ziddiyatlarini (`@ActiveProfiles({"test", "prod"})`) alohida tekshiring: `mvnw.cmd test -Dtest=SwaggerSecurityProfileTest`. (`application-prod.yml` da `springdoc.api-docs.enabled: false` bo'lgani uchun prod profilida `/v3/api-docs` 404 yoki 401 qaytarishini hisobga oling).
2. **PostgreSQL Testcontainers Integratsion Testlar:**
   - `application-test.yml` da `FlywayAutoConfiguration` exclude qilingan. Shu sababli har qanday `@Testcontainers` testida `@DynamicPropertySource` ichida `registry.add("spring.autoconfigure.exclude", () -> "");` va `registry.add("spring.flyway.enabled", () -> "true");` bo'lishi shart (`LexoRankReorderPostgresIntegrationTest` namunasidek).
   - Buyruq: `mvnw.cmd -B test -Dtest=*Postgres*IntegrationTest,WorkspaceInvitationConcurrencyIntegrationTest`
3. **OpenAPI Backward Compatibility (`oasdiff`):**
   - DTO, Controller yoki Swagger annotatsiyalari o'zgarganda `OpenApiDocumentationTest` orqali yangi `docs/openapi.json` generatsiya qilinib, `swagger-v1.json` bilan solishtirilishi shart:
   - `oasdiff breaking swagger-v1.json docs/openapi.json --config .oasdiff.yaml --fail-on ERR`

### 2.2. Muhit va OS Qoidalari (Windows & Terminal Hardening)
1. **To'liq Interpreter Yo'li (`Path Hardening`):**
   - Windows muhitida `python` buyrug'ini tekshirmasdan to'g'ridan-to'g'ri chaqirmang (`C:\Windows\System32\python.exe` Windows Store stubi jarayonni stdin kutib osib qo'yadi).
   - Oldin `where.exe python` orqali tekshiring yoki aniq interpreter yo'lini ishlating (masalan: `C:\Users\<User>\AppData\Local\Programs\Python\Python311\python.exe`).
2. **PowerShell Multiline Taqiqi:**
   - PowerShell terminaliga ichida `\n` yoki murakkab qo'shtirnoqlar bo'lgan inline `python -c "..."` kodlarini uzatmang.
   - Har doim yordamchi skriptni tizimning `%TEMP%` (yoki `.gemini/scratch/`) katalogiga `.py` fayl sifatida yozib, so'ngra ishga tushiring.

### 2.3. Tashqi Kod / Zip Arxivlar va DB Migratsiyalar Diff Tekshiruvi
1. **Ko'r-ko'rona Overwrite Taqiqlanadi:**
   - Tashqaridan berilgan `.zip` arxiv yoki fayllarni loyiha ustiga tekshiruvsiz ko'chirmang.
   - O'zgarishlarni qo'llashdan oldin va keyin `git status` hamda `git diff` orqali quyidagi kritik fayllarni solishtiring:
     - `src/main/resources/application.yml` (`spring.jpa.database-platform: org.hibernate.dialect.PostgreSQLDialect` va `hibernate.dialect` o'chib ketmasligi shart!)
     - `src/main/resources/application-prod.yml` va `application-test.yml`
     - `Dockerfile` va `render.yaml`
2. **Flyway Sxema Auditi:**
   - Har bir yangi `V<N>__*.sql` migratsiya yozishdan oldin maqsadli jadvalning haqiqiy ustunlarini `docs/PROJECT_MAP.md`, `V1`..`V62` migratsiyalari va JPA Entity (`model/*.java`) bilan solishtiring.
   - PostgreSQL indeks va triggerlarida `IF NOT EXISTS` / `DROP TRIGGER IF EXISTS` ishlating hamda soft-delete jadvallarda `WHERE deleted_at IS NULL` partial index shartini unutmang.

### 2.4. Render Cloud Production Cheklovlari (Render Free / Starter Guardrails)
- **Xotira va CPU (`Dockerfile` & `render.yaml`):** Konteyner `512MB RAM` va `0.1 CPU` muhitida ishlaydi (`-XX:MaxRAMPercentage=75.0 -XX:TieredStopAtLevel=1`). Xotizada katta ro'yxatlarni (`findAll()`) saqlamang, har doim DB darajasida sahifalash (`Pageable`) va `@Cacheable` (`CacheConfig`) ishlating.
- **HikariCP Connection Pool (`maximum-pool-size: 5`):** Tashqi HTTP chaqiruvlar (Telegram Bot API, Brevo Email API, S3) ketayotgan metodlar ustiga `@Transactional` qo'ymang — aks holda 5 ta DB ulanishi to'lib qolib, butun backend javob bermay qoladi (`Connection is not available`).
- **Fayl Saqlash (`STORAGE_TYPE=db`):** Render Free disk efemer (vaqtinchalik) bo'lgani uchun prod'da `storage.type=db` (`DatabaseFileStorageService` -> `ByteArrayResource`) ishlatiladi. Hech qachon `resource.getFile()` ga tayanmang (`MediaTypeFactory.getMediaType(resource)` ishlating).

### 2.5. Kuzatuvchanlik va CI Diagnostikasi (Zero-Guessing Logging)
- GitHub Actions'ning `/jobs/{id}/logs` endpointi ochiq repozitoriyalarda ham avtorizatsiya (`Sign in to view logs`) talab qiladi — brauzer yoki `curl` orqali xom loglarni taxminan o'qishga vaqt sarflamang.
- CI xatolarini GitHub API'ning ochiq `/commits/<sha>/check-runs` endpointidagi `output.annotations` (`::error file=...,title=...::`) yoki `$GITHUB_STEP_SUMMARY` orqali o'qing.
- **Xavfsizlik:** Ichki test loglarini `dpaste.org` kabi tashqi ochiq servislarga yuklamang (`actions/upload-artifact` va GitHub Step Summary yetarli).

### 2.6. Repozitoriy Tozaligi (Zero Repo Pollution)
- Loyiha ildizida (`root`) vaqtinchalik utilitalar (`oasdiff.exe`, `oasdiff.tar.gz`, `LICENSE`), commit xabarlari (`commit_msg*.txt`), test skriptlar (`test_*.py`) yoki log fayllarni qoldirmang.
- Barcha vaqtinchalik fayllar faqat `%TEMP%` yoki `.gitignore` ga kiritilgan `.gemini/scratch/` katalogida yaratilishi va ish yakunida tozalanishi shart.

---

## 3. Kodlash va Arxitektura Konvensiyalari

- **Dependency Injection:** Faqat constructor injection (`@Autowired` field injection taqiqlanadi). Circular dependency bo'lsa (masalan, `TelegramBotConfig` ↔ `TelegramBotService`), `@Lazy` faqat konstruktor parametrida ishlatiladi.
- **API Javob Formatlari:**
  - Muvaffaqiyatli javoblar: `ApiResponse.success("ok", data)`
  - Xatolar: `@RestControllerAdvice` (`GlobalExceptionHandler`) orqali `ApiError` qaytariladi (stack trace hech qachon tashqariga chiqarilmaydi).
  - Ro'yxatlar: `Pageable` (`page`, `size`, `sort`) bilan sahifalanadi.
- **Validatsiya:** Har bir mutating (`POST`, `PUT`, `PATCH`) endpointda `@Valid @RequestBody` bo'lishi shart.
- **Ma'lumotlar Bazasi va N+1 Oldini Olish:**
  - Ko'pga-ko'p (`assignees`, `labels`, `directions`, `watchers`) bog'lanishlarda Cartesian Product portlashini oldini olish uchun `@EntityGraph` ni `@BatchSize(size = 25)` yoki ikki bosqichli ID pagination (`findIdsByWorkspaceIdFiltered` → `findByIdIn`) bilan birga ishlating.
  - Vaqt mintaqasi: Bazaga yoziladigan timestamp'lar `UTC` (`AppTime.nowUtc()`), foydalanuvchi kunlik sanalari va cron'lar `Asia/Tashkent` (`AppTime.today()`).
- **Keshlash (`Caffeine`):**
  - `CacheConfig` dagi keshlar (`users`, `workspaces`, `workspaceRoles`, `columns`, `labels`, `sprints`) o'zgartirilganda `CacheInvalidationService` yoki `@CacheEvict` orqali tozalanishi shart.
- **Telegram Mini App (`/app/`):**
  - `src/main/resources/static/app/` ichidagi JS/CSS o'zgartirilsa, `index.html` dagi `?v=<N>` versiya raqamini yangilang (Telegram WebView keshini yangilash uchun).
  - `SecurityConfig` dagi Content-Security-Policy (`script-src 'self' ...`) talabi sababli `index.html` ichida inline `onclick="..."` yozmang; barcha hodisalarni `data-action="..."` orqali `ui-bind.js` da bog'lang.
  - Himoyalangan fayllarni yuklab olishda `window.open()` emas (`Authorization` header yubormagani uchun 401 beradi), `fetch()` + `Blob` (`downloadTaskAttachment`) funksiyasidan foydalaning.

---

## 4. Buyruqlar Ma'lumotnomasi

| Amal | Linux / macOS | Windows (CMD / PowerShell) |
| :--- | :--- | :--- |
| Kompilyatsiya | `./mvnw -q test-compile` | `mvnw.cmd -q test-compile` |
| Barcha testlar (CI) | `./mvnw -B clean verify` | `mvnw.cmd -B clean verify` |
| Yagona test klassi | `./mvnw test -Dtest=TaskServiceTest` | `mvnw.cmd test -Dtest=TaskServiceTest` |
| Lokal ishga tushirish | `./mvnw spring-boot:run` | `mvnw.cmd spring-boot:run` |
| OpenAPI eksport | `./scripts/export-openapi.sh` | `scripts\export-openapi.bat` |
| Loyiha xaritasini yangilash | `python3 scripts/generate-project-map.py` | `python scripts\generate-project-map.py` |
| Swagger UI (dev) | `http://localhost:8080/swagger-ui.html` | `http://localhost:8080/swagger-ui.html` |
| Mini App UI | `http://localhost:8080/app/` | `http://localhost:8080/app/` |
