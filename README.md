# Task Center Backend

O'zbek tilida yozilgan Spring Boot asosidagi autentifikatsiya va Jira-darajasidagi vazifalar boshqaruvi API. Java juniorlar uchun sodda va tushunarli qilib tayyorlangan.

## Loyiha haqida

`task_center_backend` — foydalanuvchilarni ro'yxatdan o'tkazish (`register`) va tizimga kirish (`login`) uchun JWT token beruvchi, shuningdek to'liq Jira/Kanban vazifalar boshqaruvi imkoniyatlariga ega backend. Swagger orqali osongina test qilish mumkin.

Asosiy imkoniyatlar:
- `POST /api/auth/register` — yangi foydalanuvchi yaratish
- `POST /api/auth/login` — tizimga kirish va JWT olish
- **Workspaces & A'zolar boshqaruvi**: a'zolarni taklif qilish (`POST /api/workspaces/{id}/members`), rollarni o'zgartirish (`OWNER`, `ADMIN`, `MEMBER`, `VIEWER`), jamoadan chiqarish
- **Kanban doska & Vazifalar**: ustunlar tartibi, vazifalar muhimligi (`Priority`: LOW, MEDIUM, HIGH, URGENT), turlari (`IssueType`: TASK, BUG, STORY, EPIC) va muddati (`dueDate`)
- **Izohlar tizimi (Comments)**: task ichida munozaralar olib borish, sahifalash (`Pageable`), soft delete
- **O'zgarishlar tarixi (Activity Log / History)**: kim, qachon, nimani o'zgartirganini kuzatish (`/api/tasks/{id}/activities`)
- **Fayllar va rasmlar (Attachments)**: taskka fayl yuklash, yuklab olish va o'chirish (`/api/tasks/{id}/attachments`)
- PostgreSQL bilan ishlaydi (Flyway migratsiyalari bilan)
- Swagger UI (springdoc-openapi) mavjud
- JWT (jjwt 0.11.5) bilan himoyalangan

## Texnologiyalar

| Texnologiya | Versiya |
|---|---|
| Java | 17 (Temurin 17.0.19+) |
| Spring Boot | 3.2.5 |
| Spring Security + JPA | 6.2.4 / Hibernate 6.4.4 |
| PostgreSQL | 16/17 |
| Flyway | 10.x |
| JWT | jjwt 0.11.5 |
| Swagger | springdoc-openapi 2.5.0 |
| Build | Maven (mvnw) |

## Talablar

- Java 17+ (`java -version` bilan tekshir)
- Maven (loyihada `mvnw` bor, alohida o'rnatish shart emas)
- PostgreSQL 17 (`localhost:5432`)
- IntelliJ IDEA 2026.2+ (yoki istalgan IDE)

## Tez boshlash (IntelliJ — Usul A, tavsiya)

Bu usulni sen tanlading — eng oson va juniorlarga mos.

1. **Loyihani ochish**
   ```
   File -> Open -> C:\Users\Bekmurod\Desktop\task_center_backend -> pom.xml ni tanla -> Open as Project
   ```
   O'ng pastdagi Maven import tugashini kut.

2. **PostgreSQL ni yoqish**

   Windows da Service o'chiq bo'lsa:
   - `Windows` tugmasi -> `PowerShell` -> o'ng click -> **Run as administrator**
   - Yoz:
     ```
     net start postgresql-x64-17
     ```
     `started successfully` chiqsa bo'ldi.

   Yoki qo'lda yaratilgan klaster ishlayotgan bo'lsa (bu loyihada `C:\Users\Bekmurod\pgdata_test` ishlatilgan), u avtomatik `5432` da turadi.

3. **Database yaratilganini tekshirish**

   Intellij da Terminal ochib:
   ```
   psql -h localhost -U postgres -p 5432 -l
   ```
   Ro'yxatda `taskcenter` bo'lishi kerak. Yo'q bo'lsa:
   ```
   psql -h localhost -U postgres -p 5432 -c "CREATE DATABASE taskcenter;"
   ```

4. **IntelliJ da Run qilish**

   `src/main/java/com/taskcenter/BackendApplication.java` ni och -> `main` yonidagi yashil **▶** ni bos -> **Run 'BackendApplication'**

   Logda:
   ```
   HikariPool-1 - Added connection
   Tomcat started on port 8080
   Started BackendApplication in 4.3 seconds
   ```
   chiqsa backend tayyor.

5. **Swagger ni ochish**

   ```
   http://localhost:8080/swagger-ui/index.html
   http://localhost:8080/v3/api-docs
   ```

   Swagger yuqorisidagi **Servers** dropdown da `http://localhost:8080 - Local` tanlangan bo'lsin. Agar `ngrok` tanlangan bo'lsa, `Failed to fetch` beradi — Local ga o'tkaz.

## Database ni IntelliJ da ulash

`application.yml` dagi sozlama:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskcenter
    username: postgres
    password: password
```

IntelliJ da ko'rish uchun:

1. O'ng/pastdagi **Database** panelini och (View > Tool Windows > Database)
2. `+` -> **Data Source -> PostgreSQL**
3. `Host: localhost`, `Port: 5432`, `Database: taskcenter`, `User: postgres`, `Password: password`
4. **Download** (driver) -> **Test Connection** -> `Succeeded` -> **OK**
5. `taskcenter -> Schemas -> public -> Tables -> users` ichida Swagger orqali yaratilgan userlarni ko'rasan.

## Swagger da test qilish

### Register

`POST /api/auth/register` -> `Try it out`:

```json
{
  "name": "Otabek",
  "email": "otabek@test.uz",
  "password": "Test1234!"
}
```

Javob **201**:
```json
{
  "success": true,
  "message": "Muvaffaqiyatli ro'yxatdan o'tdingiz",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": { "id": 1, "name": "Otabek", "email": "otabek@test.uz" }
  }
}
```

### Login

`POST /api/auth/login` -> `Try it out`:

```json
{
  "email": "otabek@test.uz",
  "password": "Test1234!"
}
```

Javob **200**:
```json
{
  "success": true,
  "message": "Tizimga muvaffaqiyatli kirdingiz",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": { "id": 1, "name": "Otabek", "email": "otabek@test.uz" }
  }
}
```

Token 7 kun amal qiladi (`jwt.expiration: 604800000`). Keyingi himoyalangan so'rovlarda Swagger dagi **Authorize** 🔓 tugmasini bosib `Bearer <token>` ni qo'y.

> Eslatma: `email` da `@` bo'lishi kerak (`strin.gmail.com` emas, `strin@gmail.com`). `AuthService.java:33` da email unique — takrorlansa `Email is already in use!` qaytadi.

### Yangi Jira Imkoniyatlari (API Lar)

1. **Workspace A'zolari**:
   - `GET /api/workspaces/{id}/members` — a'zolar ro'yxatini rollari bilan olish
   - `POST /api/workspaces/{id}/members` — yangi a'zo taklif qilish (`usernameOrEmail`, `role`)
   - `PATCH /api/workspaces/{id}/members/{userId}` — a'zo rolini o'zgartirish (`ADMIN`, `MEMBER`, `VIEWER`)
   - `DELETE /api/workspaces/{id}/members/{userId}` — a'zoni jamoadan chiqarish

2. **Vazifalar (Tasks)**:
   - `Priority`: `LOW`, `MEDIUM`, `HIGH`, `URGENT`
   - `IssueType`: `TASK`, `BUG`, `STORY`, `EPIC`
   - `dueDate`: tugash muddati (masalan, `2026-10-31`)

3. **Izohlar (Comments)**:
   - `GET /api/tasks/{taskId}/comments` — izohlarni sahifalab olish
   - `POST /api/tasks/{taskId}/comments` — yangi izoh yozish (`content`)
   - `PUT /api/tasks/{taskId}/comments/{commentId}` — izohni tahrirlash
   - `DELETE /api/tasks/{taskId}/comments/{commentId}` — izohni o'chirish

4. **Tarix (Activities)**:
   - `GET /api/tasks/{taskId}/activities` — task o'zgarishlar tarixi (kim qachon sarlavha, ustun, priority, turni o'zgartirgani)

5. **Fayllar (Attachments)**:
   - `POST /api/tasks/{taskId}/attachments` — taskka fayl yuklash
   - `GET /api/tasks/{taskId}/attachments` — fayllar ro'yxati
   - `GET /api/attachments/{id}/download` — yuklab olish
   - `DELETE /api/attachments/{id}` — faylni o'chirish

### Curl bilan test

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"test","email":"test@test.uz","password":"12345678"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.uz","password":"12345678"}'
```

Avtomatik script ham bor: `swagger_auth_test2.ps1`

```powershell
powershell -ExecutionPolicy Bypass -File swagger_auth_test2.ps1
```

## Telegram Mini App (TMA) Autentifikatsiyasi (Frontend uchun)

Backend Telegram Mini App uchun 2 xil autentifikatsiya usulini taqdim etadi:

### 1. Tavsiya etilgan usul (B-variant): `POST /api/auth/telegram` orqali bir marta JWT olish
Frontend Telegram Mini App yuklanganda `initData` ni yuborib JWT token oladi va keyingi barcha so'rovlarda standart `Authorization: Bearer <token>` ishlatadi. Bu usul Swagger'da asosiy usul sifatida tavsiya etilgan.

#### Qadamma-qadam login oqimi:
1. **Frontend `initData` ni oladi:**
   ```javascript
   const initData = window.Telegram?.WebApp?.initData;
   ```
2. **Backendga POST so'rov yuboradi:**
   - **Endpoint:** `POST /api/auth/telegram` (orqaga moslik uchun `POST /api/v1/auth/telegram` ham ishlaydi)
   - **Headers:** `Content-Type: application/json`
   - **Body:**
     ```json
     {
       "initData": "query_id=AAHd...&user=%7B%22id%22%3A12345678...%7D&auth_date=1620000000&hash=..."
     }
     ```
3. **Javobni qabul qilish:**
   - **200 OK (Muvaffaqiyatli login):**
     Oddiy login bilan bir xil formatda JWT token va foydalanuvchi ma'lumotlari qaytadi:
     ```json
     {
       "success": true,
       "message": "Tizimga muvaffaqiyatli kirdingiz",
       "data": {
         "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
         "refreshToken": "d8e3b8a1-7c2e-4b6a-9f1a-8c3d5e7f9a0b",
         "user": {
           "id": "u-123456",
           "name": "bekmurod",
           "fullName": "Bekmurod",
           "role": "USER",
           "avatar": null,
           "telegramChatId": 12345678
         }
       },
       "timestamp": "2026-10-12T14:40:00Z"
     }
     ```
     Olingan `token` keyingi so'rovlarda `Authorization: Bearer <token>` sarlavhasida ishlatiladi.

   - **401 UNAUTHORIZED — Akkaunt ulanmagan (`Avval botda akkauntingizni ulang`):**
     Agar Telegram foydalanuvchisi bazada topilmasa, xavfsizlik talablariga muvofiq yangi akkaunt **avtomatik ochilmaydi**.
     ```json
     {
       "timestamp": "2026-10-12T14:40:00Z",
       "status": 401,
       "error": "UNAUTHORIZED",
       "message": "Avval botda akkauntingizni ulang",
       "path": "/api/auth/telegram"
     }
     ```
     *Frontend harakati:* Foydalanuvchiga Telegram botga o'tib, bot orqali akkauntini ulashni taklif qiladi (yoki ish maydoniga taklif qilingan bo'lsa, taklif havolasi orqali ulanadi).

   - **401 UNAUTHORIZED — Yaroqsiz yoki muddati o'tgan initData:**
     HMAC-SHA256 imzosi buzilgan bo'lsa yoki `auth_date` 24 soatdan (86400 soniya) oshgan bo'lsa:
     ```json
     {
       "timestamp": "2026-10-12T14:40:00Z",
       "status": 401,
       "error": "UNAUTHORIZED",
       "message": "Invalid or expired initData",
       "path": "/api/auth/telegram"
     }
     ```

4. **Keyingi so'rovlar:**
   ```http
   GET /api/workspaces
   Authorization: Bearer eyJhbGciOiJIUzI1Ni...
   ```

---

### 2. Muqobil usul (A-variant, orqaga moslik uchun): Har so'rovda Header yuborish
Mavjud frontend kodlari buzilmasligi uchun quyidagi usul ham to'liq qo'llab-quvvatlanadi:
- Har bir so'rov sarlavhasida:
  ```http
  X-Telegram-Init-Data: <initData>
  ```
  yoki
  ```http
  Authorization: tma <initData>
  ```
*Eslatma:* Yangi ishlab chiqilayotgan frontendlar uchun har bir so'rovda butun initData ni yubormasdan, 1-usul orqali JWT olib ishlatish tavsiya etiladi.

---

## Loyiha tuzilishi

```
src/main/java/com/taskcenter/
  BackendApplication.java       # Main (run shu yerdan)
  config/
    SecurityConfig.java         # /api/auth/** va /swagger-ui/** ochiq, qolganlari JWT
    SwaggerConfig.java          # OpenAPI serverlar: localhost:8080 va ngrok
    CacheConfig.java            # Kesh va audit sozlamalari
  controller/
    AuthController.java         # POST /api/auth/register, /login
    WorkspaceController.java    # Workspaces va Kanban doska API
    WorkspaceMemberController.java # Jamoa a'zolari boshqaruvi
    TaskController.java         # Vazifalar CRUD va reorder
    CommentController.java      # Izohlar tizimi
    TaskActivityController.java # O'zgarishlar tarixi (History)
    AttachmentController.java   # Fayl yuklash va yuklab olish
    GlobalExceptionHandler.java
  dto/
    RegisterRequest.java, LoginRequest.java, AuthResponse.java, ApiResponse.java
    WorkspaceMemberInviteRequest.java, WorkspaceMemberRoleUpdateRequest.java, WorkspaceMemberResponseDto.java
    TaskCreateRequest.java, TaskUpdateRequest.java, TaskDto.java
    CommentCreateRequest.java, CommentUpdateRequest.java, CommentDto.java
    TaskActivityDto.java, AttachmentDto.java
  model/
    User.java, Workspace.java, BoardColumn.java, Task.java, Label.java
    WorkspaceMember.java, WorkspaceRole.java, Priority.java, IssueType.java
    Comment.java, TaskActivity.java, TaskActivityType.java, Attachment.java
  repository/
    UserRepository.java, WorkspaceRepository.java, ColumnRepository.java, TaskRepository.java
    WorkspaceMemberRepository.java, CommentRepository.java, TaskActivityRepository.java, AttachmentRepository.java
  service/
    AuthService.java, WorkspaceService.java, WorkspaceMemberService.java, TaskService.java
    CommentService.java, TaskActivityService.java, AttachmentService.java, FileStorageService.java, LocalFileStorageService.java
  security/
    JwtTokenProvider.java       # token generatsiya
    JwtAuthenticationFilter.java
    CustomUserDetailsService.java
src/main/resources/
  application.yml               # DB va jwt sozlamalari
  db/migration/                 # Flyway migratsiyalari (V1 - V16)
```

## Muhit o'zgaruvchilari

`application.yml` da:

```yaml
jwt:
  secret: super-secret-key-that-needs-to-be-at-least-256-bits-long...
  expiration: 604800000 # 7 kun
```

Docker da:

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/taskcenter
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: password
```

## Docker bilan ishga tushirish

```bash
docker compose up -d
# yoki
docker-compose up -d
```

`docker-compose.yml` da `postgres:16` va `backend` (eclipse-temurin:17) bor. `app.jar` ni avval `mvnw package -DskipTests` bilan build qilish kerak.

## Build

```bash
./mvnw package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
# yoki
./start.bat  # Windows
```

## Git

```bash
git clone https://github.com/otabek-4662/task_centr_backend.git
git add .
git commit -m "xabar"
git push origin master
```

## Muammolar va yechim

| Xato | Sabab | Yechim |
|---|---|---|
| `localhost refused the connection` | Backend o'chiq | `BackendApplication` ni run qil |
| `5432 CLOSED` | Postgres o'chiq | `net start postgresql-x64-17` (admin) |
| `Failed to fetch` Swagger da | Server ngrok tanlangan | Swagger Servers -> `http://localhost:8080` ni tanla |
| `Email is already in use!` | Email takror | Boshqa email bilan register qil |
| `Port 8080 already in use` | Eski jar turibdi | IntelliJ da eski run ni Stop (qizil ■) qil |

## Muallif

Otabek Sotimov — Java junior. Savollar bo'lsa Swagger dagi `Try it out` bilan test qilib, `users` jadvalidan tekshirib bor.
