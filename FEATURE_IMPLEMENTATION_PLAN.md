# FEATURE IMPLEMENTATION PLAN (Task Center Backend)

Ushbu hujjat loyihani Jira/Linear darajasiga olib chiqish uchun so'ralgan yangi imkoniyatlarni bosqichma-bosqich amalga oshirish rejasini qamrab oladi. Loyihaning konvensiyalariga (Constructor injection, ApiResponse, Pageable, N+1 oldini olish, va hk) to'liq amal qilinadi.

## Bosqich 1: Task Entity va DTO larni boyitish (Due Dates & Time Tracking)
Ushbu bosqichda barcha eng sodda (Entity ga maydonlar qo'shiladigan) o'zgarishlar qilinadi.

1. **Ma'lumotlar bazasi (Flyway V29):**
   - `tasks` jadvaliga yangi ustunlar qo'shish: `due_date` (TIMESTAMP), `story_points` (INT), `estimated_hours` (DOUBLE), `logged_hours` (DOUBLE).
2. **Model va DTO:**
   - `Task` entitysiga maydonlarni qo'shish.
   - `TaskCreateRequest` va `TaskUpdateRequest` ga `@Valid` bilan yangi fieldlarni qo'shish.
   - `TaskDto` ga qaytariladigan yangi fieldlarni va ularning holatini (`isOverdue`) hisoblovchi mantiqni (yoki getter ni) qo'shish.
3. **Repository va Service:**
   - `TaskRepository` ga muddati o'tgan (`dueDate < now()` and status != DONE) yoki bugun qilinishi kerak bo'lgan tasklarni filterlovchi query'larni qo'shish.
4. **Testlar:** `mvnw.cmd test` ni ishga tushirib sinash.

## Bosqich 2: Subtasks & Checklists (Kichik vazifalar)
Task ichida ro'yxatlar yaratish imkoniyati.

1. **Ma'lumotlar bazasi (Flyway V30):**
   - `task_checklist_items` jadvalini yaratish: `id`, `task_id`, `title`, `is_completed`, `order_index`, `created_at`.
2. **Model va DTO:**
   - `TaskChecklistItem` entitysini yaratish.
   - `ChecklistItemDto`, `ChecklistItemCreateRequest`, `ChecklistItemUpdateRequest`.
3. **Service va Controller:**
   - `TaskChecklistService` - CRUD amallari va order larni saqlash mantiqlari.
   - Barcha tasklarni `GET` qilganda checklistlarning tugatilish foizini (`completed_items / total_items`) DTO orqali hisoblab qaytarish. N+1 bo'lmasligi uchun `@EntityGraph` dan foydalanish.
4. **Testlar:** `mvnw.cmd test`

## Bosqich 3: Auth Refresh Token
Xavfsizlik va avtorizatsiya darajasini oshirish.

1. **Ma'lumotlar bazasi (Flyway V31):**
   - `refresh_tokens` jadvalini yaratish: `id`, `user_id`, `token`, `expiry_date`.
2. **Model va Security:**
   - `RefreshToken` entity.
   - `JwtTokenProvider` ga refresh token yaratish va uni validatsiya qilish funksiyalarini qo'shish.
3. **Service va Controller:**
   - `AuthService` ga `refreshToken` mantiqini qo'shish.
   - `POST /api/auth/refresh` endpointini ochish.
4. **Testlar:** `mvnw.cmd test`

## Bosqich 4: CSV/Excel Export (Hisobotlar)
Loyihaning Workspace va Sprintdagi vazifalarini ko'chirib olish.

1. **Service:**
   - `ExportService` yaratish. Ochiq kutubxonalar (masalan standart `StringBuilder` yoki `OpenCSV`) yordamida `Task` ro'yxatini CSV formatiga (String ga) o'girish.
2. **Controller:**
   - `GET /api/workspaces/{workspaceId}/export/csv` yaratish (yoki Sprint uchun alohida).
   - Qaytadigan javobda `ResponseEntity<Resource>` ishlatish, Content-Type: `text/csv` va `Content-Disposition` headerni sozlash.
3. **Testlar:** `mvnw.cmd test`

## Bosqich 5: @Mention va In-App Notifications (Eng murakkab qism)
Real-time web socket aloqalari va tizim xabarlari.

1. **Ma'lumotlar bazasi (Flyway V32):**
   - `notifications` jadvalini yaratish: `id`, `recipient_id`, `actor_id`, `message`, `reference_type` (TASK, COMMENT), `reference_id`, `is_read`, `created_at`.
2. **Model va DTO:**
   - `Notification` entity, `NotificationDto`.
3. **Mention Mantiqi:**
   - `CommentService` da comment yaratilganda Regex (masalan: `(?<=^|(?<=[^a-zA-Z0-9-_\.]))@([A-Za-z0-9_]+)`) orqali username'larni qidirish.
   - Topilgan userlar uchun avtomatik `Notification` obyektlarini yaratish.
4. **WebSocket va REST API:**
   - `GET /api/notifications` (Pageable), `PUT /api/notifications/{id}/read`, `PUT /api/notifications/read-all`.
   - `NotificationWebSocketService` - Yangi bildirishnoma saqlanganda, aynan shu userning ochiq Websocket kanaliga (`/user/queue/notifications`) xabar yuborish.
5. **Testlar:** `mvnw.cmd test`
