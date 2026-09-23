# 🚀 Task Center Backend (Jira-Grade Task Management Platform)

Spring Boot 3.2.5 + Java 17 + PostgreSQL asosida yaratilgan, Jira va Trello kabi zamonaviy vazifalar boshqaruvi va jamoaviy hamkorlik uchun mo'ljallangan kuchli backend platforma.

---

## 🌟 Asosiy Imkoniyatlar (Features)

### 🔐 1. Autentifikatsiya va Xavfsizlik (Auth & Security)
- **JWT (JSON Web Token)** orqali xavfsiz sessiya boshqaruvi (Access & Refresh token logikasi);
- **Spring Security 6** bilan himoyalangan REST endpointlar;
- **BCrypt Password Hashing**;
- **Rate Limiting Filter** (DoS hujumlaridan himoya);
- **CORS** konfiguratsiyasi (barcha frontend domenlar uchun ochiq).

### 🏢 2. Workspaces & Jamoa Boshqaruvi (Workspaces & Members Management)
- Ko'p loyihali ish maydonlari (Workspaces);
- **A'zolar boshqaruvi**: Yangi a'zolarni `username` yoki `email` orqali loyihaga taklif qilish;
- **Granulyar Ruxsatlar (Role-based access)**:
  - `OWNER` 👑 — Workspace to'liq egasi;
  - `ADMIN` 🔴 — A'zolar va loyihani to'liq boshqaruvchi;
  - `MEMBER` 🔵 — Vazifalar yaratuvchi va tahrirlovchi;
  - `VIEWER` ⚪️ — Faqat kuzatuvchi.
- `POST /api/workspaces/{id}/members` — A'zo taklif qilish;
- `PATCH /api/workspaces/{id}/members/{userId}` — A'zo rolini o'zgartirish;
- `DELETE /api/workspaces/{id}/members/{userId}` — A'zoni jamoadan chiqarish;
- `GET /api/workspaces/{id}/members` — A'zolar va ularning rollari ro'yxati.

### 📋 3. Kanban Doska & Vazifalar (Kanban Board & Tasks)
- Ustunlar (Columns) yaratish, tahrirlash va tartibini o'zgartirish (Drag & Drop Reorder);
- **Task Priority (Muhimlik darajasi)**: `LOW`, `MEDIUM`, `HIGH`, `URGENT`;
- **Issue Types (Vazifa turlari)**:
  - `TASK` 🟦 — Oddiy vazifa;
  - `BUG` 🔴 — Xatolik (Defect);
  - `STORY` 🟩 — Foydalanuvchi talabi (Feature);
  - `EPIC` 🟪 — Yirik modul/loyiha;
- **Due Date (Dedlayn)**: Vazifa tugash muddati (`LocalDate`), muddati o'tgan tasklarni tezkor filtrlash;
- Optimistic Locking (`@Version`) va Soft Delete (`deleted_at`).

### 💬 4. Izohlar Tizimi (Comments)
- Task ichida jamoaviy munozaralar olib borish;
- `POST /api/tasks/{taskId}/comments` — Izoh yozish;
- `GET /api/tasks/{taskId}/comments` — Sahifalangan izohlar ro'yxati (`Pageable`);
- `PUT /api/tasks/{taskId}/comments/{commentId}` — O'z izohini tahrirlash;
- `DELETE /api/tasks/{taskId}/comments/{commentId}` — O'chirish (muallif yoki admin);
- **N+1 muammosi yo'q**: `@EntityGraph` orqali mualliflar ma'lumotlari bitta SQL so'rovda yuklanadi.

### 📜 5. Faoliyatlar Tarixi (Activity Log / Audit Trail)
- Jiradagi **"History"** tabi backend ta'minoti;
- `GET /api/tasks/{taskId}/activities` — Xronologik tartibdagi o'zgarishlar jurnali;
- Kim, qachon, nimani o'zgartirganini avtomatik qayd etish (`TITLE_UPDATED`, `STATUS_UPDATED`, `PRIORITY_UPDATED`, `ISSUE_TYPE_UPDATED`, `DUE_DATE_UPDATED`, `TASK_CREATED`).

### 📎 6. Biriktirilgan Fayllar (Attachments)
- Rasm, skrinshot va hujjatlarni taskka yuklash (`multipart/form-data`);
- `POST /api/tasks/{taskId}/attachments` — Fayl biriktirish;
- `GET /api/tasks/{taskId}/attachments` — Biriktirilgan fayllar ro'yxati;
- `GET /api/attachments/{id}/download` — Faylni yuklab olish yoki ko'rish;
- `DELETE /api/attachments/{id}` — Faylni o'chirish;
- **Pluggable Storage**: `FileStorageService` interfeysi orqali xohlagan vaqtda MinIO yoki AWS S3 ga o'tish imkoniyati.

---

## 🛠 Texnologiyalar Staki

| Qatlam | Texnologiya |
|---|---|
| **Til** | Java 17 (Temurin 17.0.19+) |
| **Framework** | Spring Boot 3.2.5 (Web, Security, Data JPA, Validation) |
| **Ma'lumotlar bazasi** | PostgreSQL 16/17 (H2 in-memory testlar uchun) |
| **DB Migratsiyalari** | Flyway (V1 dan V16 gacha) |
| **Kesh** | Caffeine Cache (`CaffeineCacheManager`) |
| **Hujjatlashtirish** | Swagger / OpenAPI 3 (`springdoc-openapi` 2.5.0) |
| **Build & Tooling** | Maven Wrapper (`mvnw`), Lombok, MapStruct |
| **Test Framework** | JUnit 5, Mockito, AssertJ, Spring Boot Test (MockMvc) |

---

## 🚀 Ishga Tushirish

### 1. Talablar:
- Java 17+
- PostgreSQL 16+ (yoki Docker)

### 2. Sozlamalar (`application.yml` yoki Environment variables):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskcenter
    username: postgres
    password: your_password
  flyway:
    enabled: true
app:
  upload:
    dir: ./uploads
```

### 3. Kompilyatsiya va Testlarni ishga tushirish:
```bash
# Loyihani kompilyatsiya qilish
./mvnw clean compile

# Barcha 128 ta testni ishga tushirish
./mvnw test
```

### 4. Serverni yoqish:
```bash
./mvnw spring-boot:run
```

API ishga tushgach:
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 📚 API Endpointlar Xulosasi

### Autentifikatsiya:
- `POST /api/auth/register` — Ro'yxatdan o'tish
- `POST /api/auth/login` — Tizimga kirish va JWT olish

### Workspaces & Members:
- `GET /api/workspaces` — Foydalanuvchi a'zo bo'lgan workspacelar
- `POST /api/workspaces` — Yangi workspace yaratish
- `GET /api/workspaces/{id}` — Workspace tafsilotlari
- `GET /api/workspaces/{id}/members` — A'zolar ro'yxati
- `POST /api/workspaces/{id}/members` — A'zo taklif qilish
- `PATCH /api/workspaces/{id}/members/{userId}` — A'zo rolini o'zgartirish
- `DELETE /api/workspaces/{id}/members/{userId}` — A'zoni o'chirish

### Doska & Tasks:
- `GET /api/workspaces/{id}/board` — Kanban doska va kartochkalar
- `POST /api/workspaces/{id}/tasks` — Yangi task yaratish
- `GET /api/workspaces/{id}/tasks/{taskId}` — Task ma'lumotlari
- `PUT /api/workspaces/{id}/tasks/{taskId}` — Taskni tahrirlash (surish, nom, priority, type, due_date)
- `DELETE /api/workspaces/{id}/tasks/{taskId}` — Taskni o'chirish

### Izohlar (Comments):
- `GET /api/tasks/{taskId}/comments` — Izohlarni olish (`Pageable`)
- `POST /api/tasks/{taskId}/comments` — Izoh yozish
- `PUT /api/tasks/{taskId}/comments/{commentId}` — Izohni tahrirlash
- `DELETE /api/tasks/{taskId}/comments/{commentId}` — Izohni o'chirish

### Tarix (Activities):
- `GET /api/tasks/{taskId}/activities` — Task o'zgarishlar tarixi (`Pageable`)

### Fayllar (Attachments):
- `POST /api/tasks/{taskId}/attachments` — Fayl yuklash (`multipart/form-data`)
- `GET /api/tasks/{taskId}/attachments` — Fayllar ro'yxati
- `GET /api/attachments/{id}/download` — Faylni yuklab olish
- `DELETE /api/attachments/{id}` — Faylni o'chirish

---

## 🧪 Sifat va Testlar (Test Coverage)

Loyiha to'liq **TDD & BDD** tamoyillari asosida unit va integratsion testlar bilan qoplangan:
- Jami **128 ta avtomatlashtirilgan test** mavjud;
- Controllerlar uchun `MockMvc` orqali to'liq HTTP ssenariylari, validatsiyalar va xavfsizlik (`403 Forbidden`, `401 Unauthorized`, `404 Not Found`, `409 Conflict`) sinovdan o'tgan;
- N+1 muammolari bartaraf etilgan va ma'lumotlar bazasi so'rovlari optimallashtirilgan.

---

## 📄 Litsenziya
Loyiha o'quv va tijoriy maqsadlarda foydalanish uchun ochiq.
