# API Response Cleanup Tahlili — `task_center_backend`

> **Hujjat maqsadi:** Swagger (springdoc-openapi) va API response'larini tozalash, tushunarli qilish, frontend va mobil dasturchilar uchun qulay hujjatlashtirish va nomuvofiqliklarni bartaraf etish bo'yicha to'liq tahlil hisoboti.
> **Muhim tamoyil:** Biznes-mantiq va mavjud frontend (`static/app/app.js`, Mini App, Telegram Bot) integratsiyalari buzilmasligi qat'iy nazorat qilingan.

---

## 1. Umumiy Muammolar va Tahlil (A–G toifalari)

### A. Ortiqcha maydonlar va duplikatsiyalar
1. **`WorkspaceDto` vs `WorkspaceListDto`**: Ikkala DTO aynan bir xil maydonlar to'plamiga ega (`id, title, description, bgColor, ownerId, keyPrefix, createdAt, updatedAt`). Ikki alohida sinf saqlash ortiqcha va chalkashtiradi.
2. **`AuthResponse.UserDto` vs `UserDto`**: `AuthResponse` ichida ichki static `UserDto` e'lon qilingan, umumiy paketda esa `com.taskcenter.dto.UserDto` mavjud. Nomlar bir xil, ammo tarkibi turlicha.
3. **`ColumnDto.tasks`**: `GET /api/workspaces/{workspaceId}/columns` chaqirilganda har bir ustun ichida unga tegishli barcha `TaskDto` lar to'liq qaytadi. `static/app/app.js` ning 398–400-qatorlarida (`colsData.forEach(c => { if (c.tasks) tasksData.push(...c.tasks); });`) bu ma'lumot bevosita ishlatiladi, shuning uchun uni hozircha o'chirib bo'lmaydi. Ammo alohida `BoardController` mavjud bo'lib, u `ColumnWithCardsDto` qaytaradi.
4. **`ColumnWithCardsDto.cards.assignees`**: Har bir kartochka ichida to'liq `UserDto` (rol, email, vazifalar soni, status nikneymi bilan) qaytariladi. Kartochka uchun bu juda og'ir obyekt.

### B. Ortiqcha va yetishmayotgan response kodlar
1. **Yetishmayotgan xavfsizlik annotatsiyalari**: `ReportController` va `WorkspaceInvitationController` da `@SecurityRequirement(name = "bearerAuth")` tushib qolgan. Shu sababli Swagger UI da ushbu controllerlar ochiqdek ko'rinadi, lekin amalda JWT talab qilinadi va 401 qaytaradi.
2. **Yetishmayotgan response kodlar**: Springdoc aksariyat endpointlarda faqat `200 OK` (yoki `@ResponseStatus` bo'lsa `201`) ko'rsatmoqda. Haqiqiy 400 (Bad Request), 401 (Unauthorized), 403 (Forbidden), 404 (Not Found), 409 (Conflict), 413 (Payload Too Large), 429 (Too Many Requests) kodlari Swaggerda hujjatlanmagan.
3. **Frontendga keraksiz tashqi webhooklar**: `TelegramWebhookController` (`org.telegram.telegrambots.meta.api.objects.Update` ulkan obyektini kiritadi) va `WebhookController` (`/api/webhooks/github`) Swaggerda ko'rinib, sxemani ifloslantiradi. Ularni `@Hidden` bilan yashirish lozim.

### C. Xato formati (Error Response Format)
- Hozirgi xatolar `GlobalExceptionHandler` orqali markazlashgan va stack trace / SQL xatolarini chiqarmaydi.
- Biroq, qaytadigan xato obyekti formati:
  ```json
  {
    "success": false,
    "message": "Vazifa topilmadi",
    "data": null,
    "timestamp": "2026-10-07T18:00:00"
  }
  ```
- Foydalanuvchi va frontend dasturchi uchun aniq mashina o'qiydigan `code` (masalan: `TASK_NOT_FOUND`, `UNAUTHORIZED`, `VALIDATION_ERROR`) va `status` (masalan: `404`) maydonlari mavjud emas.
- Taklif: `ApiResponse` ga `code` va `status` maydonlarini qo'shish. Bu mavjud frontenddagi `success` va `message` maydonlarini saqlagan holda tushunarlilikni oshiradi.

### D. Tavsiflar (@Operation, @Schema)
- Loyihadagi DTO'larning qariyb 80% ida (`TaskDto`, `ColumnDto`, `WorkspaceDto`, `SprintDto`, `AttachmentDto`, `CommentDto`, `LabelDto`, `NotificationDto`, `WorkspaceReportDto` va b.) maydon tavsiflari (`@Schema(description, example)`) umuman mavjud emas.
- Swagger UI da frontend dasturchi har bir maydon nima ekanini va qanday qiymat kutishini ko'ra olmaydi. Barcha maydonlarga o'zbek tilida aniq tavsif va namunaviy misollar (example) qo'shilishi lozim.

### E. Nomlar nomuvofiqligi (Inconsistencies)
1. **Qidiruv parametrlari**:
   - `/api/chat/search` da parametr: `@RequestParam String query`
   - `/api/chat/public/search` va `/api/chat/direct/{otherUserId}/search` da parametr: `@RequestParam String q`
2. **Takliflar marshruti**:
   - Workspace doirasidagi takliflar: `/api/workspaces/{workspaceId}/invites` (`invites`)
   - Shaxsiy takliflar: `/api/invitations/me` (`invitations`)
3. **Ustunlarni qayta tartiblash**:
   - `PATCH /api/workspaces/{workspaceId}/columns` (body: `List<ColumnReorderItem>`)
   - `PATCH /api/workspaces/{workspaceId}/columns/reorder` (body: `List<String>`) — ikkita alohida endpoint bir xil amalni bajaradi.
4. **Vazifa biriktirish (`assignTask`)**:
   - Ham `@RequestParam(required = false) String userId`, ham `@RequestBody(required = false) TaskAssignRequest request` qabul qilinadi.
5. **Norasmiy/hazilomuz javob xabarlari**:
   - Task: `"Bosh og'riq yaratildi"`, `"Bosh og'riq yangilandi"`, `"Bosh og'riq o'chirildi"`
   - Workspace: `"G'alva yaratildi"`, `"G'alva yangilandi"`, `"G'alva o'chirildi"`
   - Parol: `"quloqqa aytiladigan so'zni tiklash..."`, `"Quloqqa aytiladigan so'z muvaffaqiyatli o'zgartirildi"`

### F. Null qiymatlar
- `TaskDto`, `ColumnDto`, `WorkspaceDto`, `SprintDto` kabi DTO larda `@JsonInclude(JsonInclude.Include.NON_NULL)` yo'q.
- Bo'sh bo'lgan maydonlar (`description: null`, `storyPoints: null`, `dueDate: null`, `estimatedHours: null`) har gal JSON da chiqadi.

### G. Sahifalash va ro'yxatlar (Pagination)
- Tizimda asosan Spring Data `Page<T>` (`content, totalElements, totalPages, size, number...`) ishlatiladi.
- Ammo `SprintController.getBacklog` da standart Pageable o'rniga `@RequestParam int page, int size` va xususiy `BacklogDto` (`tasks, totalTasks, totalStoryPoints, currentPage, totalPages`) qaytadi.

---

## 2. Har bir Endpoint bo'yicha Tahlil Jadvali

| Endpoint | Muammo | Jiddiylik | Fayl:qator | Taklif | Frontendga ta'siri |
| :--- | :--- | :---: | :--- | :--- | :---: |
| **POST** `/api/tasks/{taskId}/attachments` | `@Schema` yo'q, xato response kodlari (400, 401, 404, 413) Swaggerda ko'rsatilmagan | O'rta | [AttachmentController.java:36](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/AttachmentController.java#L36) | `AttachmentDto` ga `@Schema`, endpointga 201, 400, 401, 404 response kodlarini qo'shish | Yo'q |
| **GET** `/api/tasks/{taskId}/attachments` | Swaggerda 401, 404 kodlari yo'q | Past | [AttachmentController.java:47](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/AttachmentController.java#L47) | 401, 404 response kodlarini hujjatlashtirish | Yo'q |
| **GET** `/api/attachments/{attachmentId}/download` | Binary oqim qaytadi, Swaggerda `mediaType = "application/octet-stream"` ko'rsatilmagan | Past | [AttachmentController.java:56](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/AttachmentController.java#L56) | Swagger response media type va 404 kodini ko'rsatish | Yo'q |
| **DELETE** `/api/attachments/{attachmentId}` | Swaggerda 401, 403, 404 kodlari yo'q | Past | [AttachmentController.java:77](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/AttachmentController.java#L77) | Response kodlarini qo'shish | Yo'q |
| **POST** `/api/auth/register` & `/api/v1/auth/register` | Dublikat URL mapping (`/api/auth` va `/api/v1/auth`), `RegisterRequest` va `AuthResponse` da `@Schema` yo'q, 400, 409 kodlari yo'q | O'rta | [AuthController.java:31](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/AuthController.java#L31) | `@Schema` tavsiflari va 201, 400, 409 response kodlarini qo'shish | Yo'q |
| **POST** `/api/auth/login` & `/api/v1/auth/login` | Dublikat mapping, `LoginRequest` va `AuthResponse` da `@Schema` yo'q, 401 xatosi ko'rsatilmagan | O'rta | [AuthController.java:39](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/AuthController.java#L39) | `@Schema` va 200, 400, 401 response kodlarini qo'shish | Yo'q |
| **POST** `/api/auth/refresh` & `/api/v1/auth/refresh` | Dublikat mapping, `TokenRefreshRequest` va `TokenRefreshResponse` da `@Schema` yo'q | Past | [AuthController.java:46](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/AuthController.java#L46) | `@Schema` va 200, 400, 401 response kodlarini qo'shish | Yo'q |
| **POST** `/api/auth/logout` & `/api/v1/auth/logout` | Dublikat mapping, response kodlari yo'q | Past | [AuthController.java:53](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/AuthController.java#L53) | Response kodlarini qo'shish | Yo'q |
| **POST** `/api/auth/forgot-password` | Xabarda norasmiy ibora ("quloqqa aytiladigan so'z"), `@Schema` yo'q | O'rta | [AuthController.java:60](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/AuthController.java#L60) | Xabarni "Parolni tiklash havolasi yuborildi" deb to'g'rilash, `@Schema` qo'shish | Yo'q |
| **POST** `/api/auth/reset-password` | Xabarda norasmiy ibora ("Quloqqa aytiladigan so'z muvaffaqiyatli yangilandi"), `@Schema` yo'q | O'rta | [AuthController.java:67](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/AuthController.java#L67) | Xabarni "Parol muvaffaqiyatli yangilandi" deb to'g'rilash, `@Schema` qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/board` | `ColumnWithCardsDto` va `TaskCardDto` da `@Schema` yo'q, Swagger 401, 403, 404 ko'rsatilmagan | O'rta | [BoardController.java:28](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/BoardController.java#L28) | DTO larga `@Schema` va Swagger response kodlarini qo'shish | Yo'q |
| **POST** `/api/chat/public` | Request DTO da `@Schema` qisman, response kodlar (400, 401) ko'rsatilmagan | Past | [ChatController.java:39](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L39) | Swagger response kodlarini qo'shish | Yo'q |
| **POST** `/api/chat/direct` | Swagger 400, 401, 404 kodlari yo'q | Past | [ChatController.java:49](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L49) | Response kodlarini qo'shish | Yo'q |
| **PUT** `/api/chat/messages/{messageId}` | Swagger 400, 401, 403, 404 kodlari yo'q | Past | [ChatController.java:61](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L61) | Response kodlarini qo'shish | Yo'q |
| **DELETE** `/api/chat/messages/{messageId}` | Swagger 401, 403, 404 kodlari yo'q | Past | [ChatController.java:71](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L71) | Response kodlarini qo'shish | Yo'q |
| **GET** `/api/chat/public` | `Pageable` ga `@ParameterObject` qo'yilmagan, Swagger xato kodlari yo'q | O'rta | [ChatController.java:82](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L82) | `@ParameterObject` va Swagger response kodlarini qo'shish | Yo'q |
| **GET** `/api/chat/direct/{otherUserId}` | `Pageable` ga `@ParameterObject` qo'yilmagan, 401, 404 kodlari yo'q | Past | [ChatController.java:90](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L90) | `@ParameterObject` va Swagger kodlarini qo'shish | Yo'q |
| **POST** `/api/chat/messages/{messageId}/read` | Swagger 401, 404 kodlari yo'q | Past | [ChatController.java:102](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L102) | Response kodlarini qo'shish | Yo'q |
| **POST** `/api/chat/direct/{otherUserId}/read-all` | Javob turi `ApiResponse<Integer>`, alohida DTO siz primitiv son | Past | [ChatController.java:111](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L111) | Swagger tavsifini boyitish | Yo'q |
| **GET** `/api/chat/conversations` | Swagger 401 xatosi ko'rsatilmagan | Past | [ChatController.java:122](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L122) | Response kodlarini qo'shish | Yo'q |
| **GET** `/api/chat/users` | `ChatUserDto` va `UserDto` turlicha, Swagger kodlari yo'q | Past | [ChatController.java:132](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L132) | Response kodlarini qo'shish | Yo'q |
| **GET** `/api/chat/online` | Qaytish turi `ApiResponse<Set<String>>` (primitiv to'plam) | Past | [ChatController.java:139](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L139) | Swagger tavsifi va response kodlarini qo'shish | Yo'q |
| **POST** `/api/chat/{messageId}/reactions` | `ToggleReactionRequest` da `@Schema` yo'q, 400, 401, 404 yo'q | Past | [ChatController.java:148](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L148) | `@Schema` va Swagger response kodlarini qo'shish | Yo'q |
| **GET** `/api/chat/search` | Parametr nomi `query`, lekin boshqa qidiruvlarda `q` (nomuvofiqlik) | O'rta | [ChatController.java:160](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L160) | `@ParameterObject` va Swagger kodlarini qo'shish. Nomni kelgusida unifikatsiya qilish | Yo'q (Swagger darajasida) |
| **GET** `/api/chat/public/search` | Parametr `q`, `@ParameterObject` yo'q | Past | [ChatController.java:170](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L170) | `@ParameterObject` va Swagger kodlarini qo'shish | Yo'q |
| **GET** `/api/chat/direct/{otherUserId}/search` | Parametr `q`, `@ParameterObject` yo'q | Past | [ChatController.java:180](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ChatController.java#L180) | `@ParameterObject` va Swagger kodlarini qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/columns` | `ColumnDto` da `@Schema` yo'q, ichida `tasks` (`List<TaskDto>`) to'liq yuklanadi (`app.js` da ishlatiladi) | O'rta | [ColumnController.java:33](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ColumnController.java#L33) | `ColumnDto` ga to'liq `@Schema` qo'shish, 401, 403, 404 kodlarini hujjatlashtirish | Yo'q |
| **POST** `/api/workspaces/{workspaceId}/columns` | 201 o'rniga 200 qaytadi, `ColumnCreateRequest` da `@Schema` yo'q | O'rta | [ColumnController.java:42](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ColumnController.java#L42) | `@ResponseStatus(HttpStatus.CREATED)` (201) qo'yish, `@Schema` qo'shish | Yo'q |
| **PUT** `/api/workspaces/{workspaceId}/columns/{id}` | Swagger response kodlari yo'q | Past | [ColumnController.java:52](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ColumnController.java#L52) | Response kodlarini qo'shish | Yo'q |
| **PATCH** `/api/workspaces/{workspaceId}/columns/{id}` | `ColumnPatchRequest` da `@Schema` yo'q | Past | [ColumnController.java:63](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ColumnController.java#L63) | `@Schema` va response kodlarini qo'shish | Yo'q |
| **PATCH** `/api/workspaces/{workspaceId}/columns` & `/reorder` | Ikkita parallel reorder endpoint (`List<ColumnReorderItem>` va `List<String>`) | O'rta | [ColumnController.java:74](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ColumnController.java#L74), [L84](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ColumnController.java#L84) | Swagger tavsifida ikkala usulni aniq izohlash, kelgusida bittasiga kelishish | Yo'q |
| **DELETE** `/api/workspaces/{workspaceId}/columns/{id}` | Swagger 401, 403, 404 kodlari yo'q | Past | [ColumnController.java:94](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ColumnController.java#L94) | Response kodlarini qo'shish | Yo'q |
| **GET** `/api/tasks/{taskId}/comments` | `CommentDto` da `@Schema` yo'q, `@ParameterObject` yo'q | Past | [CommentController.java:34](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/CommentController.java#L34) | `CommentDto` ga `@Schema`, `@ParameterObject` va kodlarni qo'shish | Yo'q |
| **POST** `/api/tasks/{taskId}/comments` | `CommentCreateRequest` da `@Schema` yo'q | Past | [CommentController.java:44](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/CommentController.java#L44) | `@Schema` va 201, 400, 401, 404 kodlarini qo'shish | Yo'q |
| **PUT** `/api/tasks/{taskId}/comments/{commentId}` | `CommentUpdateRequest` da `@Schema` yo'q | Past | [CommentController.java:55](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/CommentController.java#L55) | `@Schema` va response kodlarini qo'shish | Yo'q |
| **DELETE** `/api/tasks/{taskId}/comments/{commentId}` | Swagger response kodlari yo'q | Past | [CommentController.java:66](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/CommentController.java#L66) | Response kodlarini qo'shish | Yo'q |
| **GET** `/api/v1/config/public` | `ApiResponse` ga o'ralmagan (`Map<String, String>` qaytadi). Loyiha konvensiyasiga zid | O'rta | [ConfigController.java:25](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ConfigController.java#L25) | `ApiResponse<PublicConfigDto>` ga o'rash (`app.js:221` ikkala holatni ham qo'llaydi: `res?.data?.botUsername \|\| res?.botUsername`) | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/export/csv` | Swaggerda response `text/csv` va `binary` deb ko'rsatilmagan | Past | [ExportController.java:34](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ExportController.java#L34) | Swagger media type va xato kodlarini qo'shish | Yo'q |
| **POST** `/api/files/upload` | 400, 413 (Payload Too Large) xatoliklari Swaggerda ko'rsatilmagan, `@SecurityRequirement` yo'q | Past | [FileController.java:31](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/FileController.java#L31) | 400, 413 kodlarini va `FileUploadResponse` da example qiymatlarni qo'shish | Yo'q |
| **GET** `/api/files/{fileName:.+}` | Swaggerda binary fayl turi va 404 xatosi yo'q | Past | [FileController.java:40](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/FileController.java#L40) | Swagger response tavsifini qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/labels` | `LabelDto` da `@Schema` yo'q | Past | [LabelController.java:29](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/LabelController.java#L29) | `LabelDto` ga `@Schema` va response kodlarini qo'shish | Yo'q |
| **POST** `/api/workspaces/{workspaceId}/labels` | Request body sifatida `LabelDto` olingan (`id` va `workspaceId` ortiqcha), 200 qaytadi | O'rta | [LabelController.java:39](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/LabelController.java#L39) | 201 statusini ko'rsatish, `@Schema` qo'shish | Yo'q |
| **DELETE** `/api/workspaces/{workspaceId}/labels/{id}` | Swagger response kodlari yo'q | Past | [LabelController.java:48](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/LabelController.java#L48) | Response kodlarini qo'shish | Yo'q |
| **GET** `/api/notifications` | `NotificationDto` maydonlarida `@Schema` yo'q, `@ParameterObject` yo'q | Past | [NotificationController.java:28](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/NotificationController.java#L28) | `NotificationDto` ga `@Schema`, `@ParameterObject` qo'shish | Yo'q |
| **PUT** `/api/notifications/{id}/read` & `/read-all` | Xabar shunchaki `"ok"`, response kodlari yo'q | Past | [NotificationController.java:36](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/NotificationController.java#L36), [L45](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/NotificationController.java#L45) | Xabarni aniqroq qilish ("Bildirishnoma o'qildi"), response kodlarini qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/reports/summary` | **`@SecurityRequirement(name = "bearerAuth")` YO'Q!** Swaggerda qulf belgisi yo'q, tokensiz sinalsa 401 beradi. `WorkspaceReportDto` da `@Schema` yo'q | **Yuqori** | [ReportController.java:25](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ReportController.java#L25) | `@SecurityRequirement` qo'yish, DTO larga `@Schema` va response kodlarini qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/reports/sprints/{sprintId}` | **`@SecurityRequirement` YO'Q!** `SprintReportDto` da `@Schema` yo'q | **Yuqori** | [ReportController.java:35](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/ReportController.java#L35) | `@SecurityRequirement` qo'yish, `@Schema` va response kodlarini qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/sprints` | `SprintDto` da `@Schema` yo'q | Past | [SprintController.java:30](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/SprintController.java#L30) | `SprintDto` ga `@Schema`, response kodlarini qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/sprints/velocity` | `VelocityChartDto` da `@Schema` yo'q | Past | [SprintController.java:40](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/SprintController.java#L40) | `@Schema` va response kodlarini qo'shish | Yo'q |
| **POST** `/api/workspaces/{workspaceId}/sprints` | `SprintCreateRequest` da `@Schema` yo'q, xato response kodlari yo'q | Past | [SprintController.java:50](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/SprintController.java#L50) | `@Schema` va 201, 400, 401, 403 kodlarini qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/sprints/{id}` | Swagger response kodlari yo'q | Past | [SprintController.java:60](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/SprintController.java#L60) | Response kodlarini qo'shish | Yo'q |
| **PUT** `/api/workspaces/{workspaceId}/sprints/{id}` | `SprintUpdateRequest` da `@Schema` yo'q | Past | [SprintController.java:70](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/SprintController.java#L70) | Response kodlarini qo'shish | Yo'q |
| **POST** `/api/workspaces/{workspaceId}/sprints/{id}/start` & `/complete` | Swagger xato kodlari (400, 401, 403, 404) yo'q | Past | [SprintController.java:81](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/SprintController.java#L81), [L91](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/SprintController.java#L91) | Response kodlarini qo'shish | Yo'q |
| **DELETE** `/api/workspaces/{workspaceId}/sprints/{id}` | Swagger response kodlari yo'q | Past | [SprintController.java:102](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/SprintController.java#L102) | Response kodlarini qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/sprints/{id}/tasks` & **POST** `.../tasks` & **DELETE** `.../tasks/{taskId}` | Swagger response kodlari yo'q, `SprintTaskMoveRequest` da `@Schema` yo'q | Past | [SprintController.java:112-133](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/SprintController.java#L112-L133) | `@Schema` va response kodlarini qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/backlog` | Standart `Pageable` o'rniga `@RequestParam int page, int size` va nostandart `BacklogDto` ishlatilgan | O'rta | [SprintController.java:144](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/SprintController.java#L144) | `BacklogDto` ga `@Schema` va response kodlarini qo'shish. Kelgusida Pageable ga o'tishni taklif qilish | Yo'q (Swagger darajasida) |
| **GET** `/api/tasks/{taskId}/activities` | `TaskActivityDto` da `@Schema` yo'q, `@ParameterObject` yo'q | Past | [TaskActivityController.java:30](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskActivityController.java#L30) | `TaskActivityDto` ga `@Schema`, `@ParameterObject` qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists` | `ChecklistItemDto` da `@Schema` yo'q | Past | [TaskChecklistController.java:29](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskChecklistController.java#L29) | `@Schema` va response kodlarini qo'shish | Yo'q |
| **POST**, **PUT**, **DELETE** `.../checklists` | Xabarlar qisqa `"ok"`, `"o'chirildi"`, request DTO larda `@Schema` yo'q | Past | [TaskChecklistController.java:38-60](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskChecklistController.java#L38-L60) | Xabarlarni aniq qilish, `@Schema` va response kodlarini qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/tasks` | `TaskDto` da birorta maydonda `@Schema` yo'q, Swagger 401, 403, 404 yo'q | O'rta | [TaskController.java:36](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskController.java#L36) | `TaskDto` ga to'liq `@Schema` (o'zbekcha tavsif, namunalar), response kodlarini qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/tasks/{id}` | Swagger response kodlari yo'q | Past | [TaskController.java:47](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskController.java#L47) | Response kodlarini qo'shish | Yo'q |
| **POST** `/api/workspaces/{workspaceId}/tasks` | Nojo'ya xabar: `"Bosh og'riq yaratildi"`, 201 qaytmaydi (200), `TaskCreateRequest` da `@Schema` yo'q | O'rta | [TaskController.java:57](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskController.java#L57) | Xabarni `"Vazifa muvaffaqiyatli yaratildi"` ga o'zgartirish, `@ResponseStatus(HttpStatus.CREATED)` va `@Schema` qo'shish | Yo'q |
| **PUT** `/api/workspaces/{workspaceId}/tasks/{id}` | Nojo'ya xabar: `"Bosh og'riq yangilandi"`, `TaskUpdateRequest` da `@Schema` yo'q | O'rta | [TaskController.java:67](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskController.java#L67) | Xabarni `"Vazifa muvaffaqiyatli yangilandi"` ga o'zgartirish, `@Schema` qo'shish | Yo'q |
| **PATCH** `/api/workspaces/{workspaceId}/tasks/{id}` | Swagger response kodlari yo'q | Past | [TaskController.java:78](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskController.java#L78) | Response kodlarini qo'shish | Yo'q |
| **PATCH** `/api/workspaces/{workspaceId}/tasks/{id}/reorder` | `TaskReorderRequest` da `@Schema` yo'q | Past | [TaskController.java:89](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskController.java#L89) | `@Schema` va response kodlarini qo'shish | Yo'q |
| **POST** `/api/workspaces/{workspaceId}/tasks/{id}/watch` | Swagger response kodlari yo'q | Past | [TaskController.java:100](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskController.java#L100) | Response kodlarini qo'shish | Yo'q |
| **DELETE** `/api/workspaces/{workspaceId}/tasks/{id}` | Nojo'ya xabar: `"Bosh og'riq o'chirildi"` | O'rta | [TaskController.java:110](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskController.java#L110) | Xabarni `"Vazifa muvaffaqiyatli o'chirildi"` ga o'zgartirish, response kodlarini qo'shish | Yo'q |
| **POST** `/api/workspaces/{workspaceId}/tasks/{id}/archive` & `/unarchive` | Swagger response kodlari yo'q | Past | [TaskController.java:120](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskController.java#L120), [L130](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskController.java#L130) | Response kodlarini qo'shish | Yo'q |
| **POST** `/api/workspaces/{workspaceId}/tasks/{id}/assign` | Ham `@RequestParam userId`, ham `@RequestBody TaskAssignRequest` qabul qilinadi (Swaggerda chalkash) | **Yuqori** | [TaskController.java:140](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskController.java#L140) | Swaggerda `@Parameter` tavsiflarini aniq yozish (`app.js:204` query param ishlatadi, shuning uchun uni saqlash shart) | Yo'q |
| **GET** `/api/tasks/{id}` | Alohida "Tasks Direct" tagida turibdi, `TaskController` dagi bilan bir xil vazifa | Past | [TaskDirectController.java:26](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TaskDirectController.java#L26) | Response kodlarini qo'shish | Yo'q |
| **POST** `/api/v1/auth/telegram` | `TelegramAuthRequest` da `@Schema` yo'q, 401 Unauthorized ko'rsatilmagan | O'rta | [TelegramAuthController.java:50](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TelegramAuthController.java#L50) | `TelegramAuthRequest` ga `@Schema`, Swagger 200, 401 kodlarini qo'shish | Yo'q |
| **POST** `/api/webhooks/telegram` | Ichki bot webhooki, `Update` klassi Swagger schemalarini ifloslantirib yuborgan | **Yuqori** | [TelegramWebhookController.java:20](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/TelegramWebhookController.java#L20) | Controllerga `@Hidden` qo'yish (Swaggerdan yashirish) | Yo'q |
| **GET** `/api/me` & `/api/auth/me` | Ikkala URL ham mavjud (alias), Swaggerda dublikat | Past | [UserController.java:41](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/UserController.java#L41), [L47](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/UserController.java#L47) | Bitta asosiy endpointni ko'rsatib, aliasga `@Hidden` qo'yish | Yo'q |
| **GET** `/api/users/me/tasks` & `/api/me/tasks` | URL dublikatsiya | Past | [UserController.java:53](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/UserController.java#L53) | Swaggerda izoh berish | Yo'q |
| **GET** `/api/users` | Swagger response kodlari yo'q, `@ParameterObject` yo'q | Past | [UserController.java:63](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/UserController.java#L63) | `@ParameterObject` va response kodlarini qo'shish | Yo'q |
| **GET** `/api/users/me/telegram-link-token` | Swagger response kodlari yo'q | Past | [UserController.java:73](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/UserController.java#L73) | Response kodlarini qo'shish | Yo'q |
| **DELETE** `/api/users/me/telegram` | Swagger response kodlari yo'q | Past | [UserController.java:79](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/UserController.java#L79) | Response kodlarini qo'shish | Yo'q |
| **POST** `/api/users/me/telegram` | Rate limit (429 Too Many Requests) Swaggerda hujjatlanmagan, `TelegramLinkRequest` da `@Schema` yo'q | O'rta | [UserController.java:88](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/UserController.java#L88) | 429 xatosi va `@Schema` ni qo'shish | Yo'q |
| **PATCH** `/api/users/me` | `UpdateProfileRequest` da `@Schema` yo'q | Past | [UserController.java:100](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/UserController.java#L100) | `@Schema` va response kodlarini qo'shish | Yo'q |
| **POST** `/api/users/me/change-password` | Nojo'ya xabar: "Quloqqa aytiladigan so'z muvaffaqiyatli o'zgartirildi", `ChangePasswordRequest` da `@Schema` yo'q | O'rta | [UserController.java:110](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/UserController.java#L110) | Xabarni "Parol muvaffaqiyatli o'zgartirildi" ga tuzatish, `@Schema` va kodlarni qo'shish | Yo'q |
| **POST** `/api/webhooks/github` | Tashqi GitHub webhook, frontendchiga kerak emas | **Yuqori** | [WebhookController.java:17](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WebhookController.java#L17) | Controllerga `@Hidden` qo'yish (Swaggerdan yashirish) | Yo'q |
| **GET** `/api/workspaces` | `WorkspaceListDto` maydonlarida `@Schema` yo'q, `@ParameterObject` yo'q | Past | [WorkspaceController.java:36](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceController.java#L36) | `@Schema`, `@ParameterObject` va response kodlarini qo'shish | Yo'q |
| **GET** `/api/workspaces/{id}` | `WorkspaceDto` da `@Schema` yo'q | Past | [WorkspaceController.java:45](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceController.java#L45) | `WorkspaceDto` ga `@Schema`, response kodlarini qo'shish | Yo'q |
| **POST** `/api/workspaces` | Nojo'ya xabar: `"G'alva yaratildi"`, 201 qaytmaydi, `WorkspaceCreateRequest` da `@Schema` yo'q | O'rta | [WorkspaceController.java:54](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceController.java#L54) | Xabarni `"Workspace muvaffaqiyatli yaratildi"` deb to'g'rilash, `@ResponseStatus(HttpStatus.CREATED)` va `@Schema` qo'shish | Yo'q |
| **PUT** `/api/workspaces/{id}` | Nojo'ya xabar: `"G'alva yangilandi"` | O'rta | [WorkspaceController.java:63](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceController.java#L63) | Xabarni `"Workspace muvaffaqiyatli yangilandi"` deb to'g'rilash | Yo'q |
| **DELETE** `/api/workspaces/{id}` | Nojo'ya xabar: `"G'alva o'chirildi"` | O'rta | [WorkspaceController.java:73](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceController.java#L73) | Xabarni `"Workspace muvaffaqiyatli o'chirildi"` deb to'g'rilash | Yo'q |
| **POST** `/api/workspaces/{workspaceId}/invites` | **`@SecurityRequirement(name = "bearerAuth")` YO'Q!** Swaggerda qulf belgisi chiqmaydi. `InviteRequestDto` va `WorkspaceInvitationDto` da `@Schema` yo'q | **Yuqori** | [WorkspaceInvitationController.java:28](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceInvitationController.java#L28) | Controllerga `@SecurityRequirement` qo'shish, `@Schema` va 201, 400, 401, 403, 404 kodlarini qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/invites` | `@SecurityRequirement` yo'q | **Yuqori** | [WorkspaceInvitationController.java:38](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceInvitationController.java#L38) | `@SecurityRequirement` va response kodlarini qo'shish | Yo'q |
| **DELETE** `/api/workspaces/{workspaceId}/invites/{id}` | `@SecurityRequirement` yo'q | **Yuqori** | [WorkspaceInvitationController.java:46](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceInvitationController.java#L46) | `@SecurityRequirement` va response kodlarini qo'shish | Yo'q |
| **GET** `/api/invitations/me` | Nom nomuvofiqligi (`/invitations/me` vs `/workspaces/{ws}/invites`). DTO da workspace nomi yo'q (faqat id), `@SecurityRequirement` yo'q | **Yuqori** | [WorkspaceInvitationController.java:56](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceInvitationController.java#L56) | `@SecurityRequirement` va response kodlarini qo'shish. Kelgusida DTO ga `workspaceTitle` qo'shish | Yo'q |
| **PATCH** `/api/invitations/{id}/accept` & `/reject` | `@SecurityRequirement` yo'q, Swagger kodlari yo'q | **Yuqori** | [WorkspaceInvitationController.java:63](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceInvitationController.java#L63), [L72](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceInvitationController.java#L72) | `@SecurityRequirement` va response kodlarini qo'shish | Yo'q |
| **GET** `/api/workspaces/{workspaceId}/members` | `WorkspaceMemberResponseDto` da `@Schema` yo'q | Past | [WorkspaceMemberController.java:32](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceMemberController.java#L32) | `WorkspaceMemberResponseDto` ga `@Schema`, response kodlarini qo'shish | Yo'q |
| **POST** `/api/workspaces/{workspaceId}/members` | `WorkspaceMemberInviteRequest` da `@Schema` yo'q | Past | [WorkspaceMemberController.java:41](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceMemberController.java#L41) | `@Schema` va 201, 400, 401, 403 kodlarini qo'shish | Yo'q |
| **PATCH** `/api/workspaces/{workspaceId}/members/{userId}` | `WorkspaceMemberRoleUpdateRequest` da `@Schema` yo'q | Past | [WorkspaceMemberController.java:52](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceMemberController.java#L52) | `@Schema` va response kodlarini qo'shish | Yo'q |
| **DELETE** `/api/workspaces/{workspaceId}/members/{userId}` | Swagger response kodlari yo'q | Past | [WorkspaceMemberController.java:63](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/controller/WorkspaceMemberController.java#L63) | Response kodlarini qo'shish | Yo'q |

---

## 3. O'zgarishlar Tasnifi (Xavfsiz va Xavfli)

### 1-RO'YXAT: XAVFSIZ O'ZGARISHLAR (Frontend sinmaydi)
> Ushbu o'zgarishlar faqat Swagger UI, OpenAPI metadata, xatolik response kodlari, tushuntirishlar va xabar matnlariga tegishli. Frontend JavaScript kodiga (`app.js` va botlarga) hech qanday salbiy ta'sir ko'rsatmaydi.

1. **Swagger xavfsizligini to'g'rilash (`@SecurityRequirement`)**:
   - `ReportController` va `WorkspaceInvitationController` ga `@SecurityRequirement(name = "bearerAuth")` qo'shish (Swagger UI da Authorize qulfi paydo bo'ladi).
2. **Ortiqcha tashqi webhooklarni Swaggerdan yashirish (`@Hidden`)**:
   - `TelegramWebhookController` ga `@Hidden` qo'yish (Telegram Bot kutubxonasining ulkan `Update` sxemasi Swaggerdan yo'qoladi).
   - `WebhookController` (`/api/webhooks/github`) ga `@Hidden` qo'yish.
3. **Alias endpointlarni Swaggerdan yashirish (`@Hidden`)**:
   - `/api/auth/me` (`/api/me` mavjud) va `/api/me/tasks` (`/api/users/me/tasks` mavjud) ga `@Hidden` qo'yish orqali duplikatsiyani yo'qotish.
4. **Har bir endpointga to'g'ri HTTP response kodlarini qo'shish**:
   - Har bir endpointga haqiqatda bo'lishi mumkin bo'lgan kodlarni (`@ApiResponse(responseCode = "200/201/400/401/403/404/429")`) qo'shish.
   - Resurs yaratuvchi POST endpointlarga `@ResponseStatus(HttpStatus.CREATED)` qo'yish (`ColumnController.createColumn`, `TaskController.createTask`, `WorkspaceController.createWorkspace`, `LabelController.createLabel`).
5. **DTO maydonlariga o'zbek tilida to'liq `@Schema` qo'shish**:
   - `TaskDto`, `TaskCreateRequest`, `TaskUpdateRequest`, `TaskReorderRequest`
   - `ColumnDto`, `ColumnCreateRequest`, `ColumnPatchRequest`, `ColumnWithCardsDto`
   - `WorkspaceDto`, `WorkspaceCreateRequest`, `WorkspaceListDto`
   - `WorkspaceMemberResponseDto`, `WorkspaceMemberInviteRequest`, `WorkspaceMemberRoleUpdateRequest`
   - `WorkspaceInvitationDto`, `InviteRequestDto`
   - `SprintDto`, `SprintCreateRequest`, `SprintUpdateRequest`, `VelocityChartDto`, `BacklogDto`
   - `AttachmentDto`, `CommentDto`, `LabelDto`, `NotificationDto`
   - `WorkspaceReportDto`, `SprintReportDto`, `UserWorkloadDto`
   - `AuthResponse`, `LoginRequest`, `RegisterRequest`, `TokenRefreshRequest`, `TokenRefreshResponse`
   - `TelegramAuthRequest`, `TelegramLinkRequest`
6. **Nojo'ya/hazilomuz xabarlarni rasmiy va tushunarli qilish**:
   - `"Bosh og'riq yaratildi/yangilandi/o'chirildi"` ➔ `"Vazifa muvaffaqiyatli yaratildi/yangilandi/o'chirildi"`
   - `"G'alva yaratildi/yangilandi/o'chirildi"` ➔ `"Workspace muvaffaqiyatli yaratildi/yangilandi/o'chirildi"`
   - `"quloqqa aytiladigan so'z..."` ➔ `"Parol muvaffaqiyatli yangilandi / tiklash havolasi yuborildi"`
7. **`Pageable` parametrlariga `@ParameterObject` qo'shish**:
   - Swagger UI da `page`, `size`, `sort` parametrlarini chiroyli va tushunarli ko'rsatish.
8. **`ConfigController` ni `ApiResponse` ga o'rash**:
   - `ResponseEntity<Map<String, String>>` o'rniga `ApiResponse<PublicConfigDto>` qaytarish (`app.js:221` allaqachon `res?.data?.botUsername || res?.botUsername` ni qo'llab-quvvatlaydi).
9. **`ApiResponse` ga `code` va `status` maydonlarini qo'shish (`@JsonInclude(NON_NULL)`)**:
   - Mavjud `success`, `message`, `data`, `timestamp` maydonlari tegilmaydi, yangi maydonlar faqat xato bo'lganda (`ApiResponse.error(code, message, status)`) to'ldiriladi.

---

### 2-RO'YXAT: XAVFLI O'ZGARISHLAR (Frontend o'zgarishi kerak, kelishuv zarur)
> Ushbu o'zgarishlar JSON maydon nomlarini, qaytish strukturasini yoki URL yo'llarini o'zgartiradi. Agar bular hozir bajarilsa, `static/app/app.js` yoki Telegram bot kodlari ishlashdan to'xtashi mumkin. **Frontend jamoasi bilan kelishilgandan so'ng amalga oshirilishi lozim.**

1. **`ColumnDto` ichidan `tasks` ro'yxatini olib tashlash**:
   - *Nega xavfli:* `app.js:399` da `colsData.forEach(c => { if (c.tasks) tasksData.push(...c.tasks); })` mavjud. Agar `c.tasks` olib tashlansa, frontend doskada vazifalar ko'rinmay qoladi. Frontend doskani yuklash uchun `/api/workspaces/{ws}/board` ga o'tishi kerak.
2. **`BacklogDto` formatini standart `Page<TaskDto>` ga o'tkazish**:
   - *Nega xavfli:* Hozirgi `BacklogDto` da `currentPage, totalPages, totalTasks` mavjud. Agar `Page` ga o'tkazilsa, frontend ushbu maydonlarni `number, totalElements` deb o'qishi kerak bo'ladi.
3. **Qidiruv parametrini unifikatsiya qilish (`query` ➔ `q`)**:
   - `/api/chat/search` da `query` o'rniga `q` qabul qilish. Agar frontend allaqachon `?query=` deb so'rov yuborayotgan bo'lsa, qidiruv ishlamaydi.
4. **Takliflar marshrutini unifikatsiya qilish (`/invites` vs `/invitations`)**:
   - `/api/workspaces/{ws}/invites` va `/api/invitations/me` ni bitta umumiy nom ostiga keltirish. Frontend endpoint chaqiruvlarini o'zgartirishi kerak.
5. **`WorkspaceInvitationDto` ga `workspaceTitle` va `senderName` qo'shish**:
   - Bu qo'shimcha maydon bo'lib, o'zi xavfsiz, lekin entity relation (JOIN) talab qilishi mumkin.
6. **`TaskController.assignTask` endpointini tozalash**:
   - Hozirgi ikkita parametr (`@RequestParam userId` va `@RequestBody TaskAssignRequest`) dan birini tanlash. `app.js:204` da `?userId=` query parametri ishlatilgani sababli, `@RequestBody` ga o'tilsa, frontend o'zgarishi kerak bo'ladi.
7. **Ustunlarni reorder qilishning ikkita endpointini bittaga keltirish**:
   - `PATCH /columns` (`List<ColumnReorderItem>`) yoki `PATCH /columns/reorder` (`List<String>`) lardan birini qoldirish.
