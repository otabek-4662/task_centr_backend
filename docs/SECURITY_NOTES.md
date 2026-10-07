# Security Notes: Controller Xavfsizlik Tekshiruvi

Ushbu hujjat `ReportController`, `WorkspaceInvitationController`, `TelegramWebhookController` va `WebhookController` xavfsizlik arxitekturasi bo'yicha mustaqil tekshiruv hisoboti.

---

## 1. ReportController va WorkspaceInvitationController

### 1.1. SecurityConfig da autentifikatsiya (Authenticated) ostidami?
**Ha, ikkala controller ham to'liq autentifikatsiya ostida.**

- **SecurityConfig sozlamalari (`SecurityConfig.java:143-145`):**
  ```java
  .requestMatchers("/api/workspaces/**").hasAnyRole("USER", "ADMIN")
  .anyRequest().authenticated()
  ```
- **ReportController:**
  - Barcha endpointlar `/api/workspaces/{workspaceId}/reports/**` marshruti ostida joylashgan.
  - Bu `/api/workspaces/**` qoidasiga to'g'ri keladi va `USER` yoki `ADMIN` roli (ya'ni haqiqiy JWT token) talab qiladi.
- **WorkspaceInvitationController:**
  - Workspace takliflari (`/api/workspaces/{workspaceId}/invites/**`) `/api/workspaces/**` orqali himoyalangan.
  - Shaxsiy takliflar (`/api/invitations/me`, `/api/invitations/{id}/accept`, `/api/invitations/{id}/reject`) `anyRequest().authenticated()` orqali JWT token talab qiladi.

### 1.2. Workspace a'zoligi va taklif egaligi tekshiriladimi?
**Ha, servis qatlamida har bir operatsiya uchun alohida ruxsat tekshiruvi (authorization check) amalga oshiriladi.**

1. **`ReportService`:**
   - `getWorkspaceSummary`: `authorizationService.checkAccess(workspaceId, currentUser)` chaqiriladi. Workspace ga a'zo bo'lmagan foydalanuvchiga `ForbiddenException` beriladi.
   - `getSprintSummary`: `authorizationService.checkAccess(workspaceId, currentUser)` va sprint aynan shu workspace ga tegishliligi tekshiriladi (`sprint.getWorkspaceId().equals(workspaceId)`).

2. **`WorkspaceInvitationService`:**
   - **Taklif yuborish / ko'rish / bekor qilish:** `authorizationService.checkAdmin(workspaceId, currentUser)` chaqiriladi. Faqat workspace `OWNER` yoki `ADMIN` lari taklif yuborishi, ko'rishi va bekor qilishi mumkin.
   - **Taklifni qabul qilish (`acceptInvitation`) va rad etish (`rejectInvitation`):**
     ```java
     boolean isAuthorized = false;
     if (invitation.getReceiverId() != null) {
         isAuthorized = invitation.getReceiverId().equals(currentUser.getId());
     } else if (invitation.getReceiverEmail() != null) {
         isAuthorized = invitation.getReceiverEmail().equalsIgnoreCase(currentUser.getEmail());
     }
     if (!isAuthorized) {
         throw new ForbiddenException("Bu taklif sizga tegishli emas");
     }
     ```
     Boshqa foydalanuvchining taklifini qabul qilish yoki rad etish mutlaqo imkonsiz.
   - **Mening takliflarim (`getMyPendingInvitations`):** Foydalanuvchiga faqat o'zining `id` siga yuborilgan va `PENDING` holatdagi takliflargina qaytariladi.

---

## 2. TelegramWebhookController va WebhookController

### 2.1. TelegramWebhookController (Secret Token tekshiruvi)
- **Header:** `X-Telegram-Bot-Api-Secret-Token`
- **Mantiq (`TelegramWebhookController.java:41-46`):**
  ```java
  if (expectedSecret != null && !expectedSecret.isBlank()) {
      if (secretToken == null || !expectedSecret.trim().equals(secretToken.trim())) {
          log.warn("Telegram Webhook xavfsizlik xatosi: noto'g'ri secret token");
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
  }
  ```
- **Xulosa:**
  - Agar `telegram.bot.webhook-secret` env o'zgaruvchisi belgilangan bo'lsa, Telegram dan kelgan token to'liq tekshiriladi va xato bo'lsa `401 Unauthorized` qaytadi.
  - **Tavsiya:** Production muhitda `TELEGRAM_BOT_WEBHOOK_SECRET` bo'sh bo'lmasligi shart.

### 2.2. WebhookController (GitHub Push Signature tekshiruvi)
- **Header:** `X-Hub-Signature-256`
- **Mantiq (`WebhookService.java:63-83`):**
  - `webhookSecret` bo'sh bo'lsa, webhook so'rovi darhol rad etiladi (`ForbiddenException("Webhook sozlanmagan")`).
  - `signatureHeader` mavjudligi va `sha256=` prefiksi bilan boshlanishi tekshiriladi.
  - Xom so'rov tanasi (`byte[] rawBody`) ustidan `HmacSHA256` orqali imzo hisoblanadi.
  - Hisoblangan imzo va kelgan imzo `MessageDigest.isEqual` orqali taqqoslanadi (timing-attack xakerlik urinishlariga qarshi xavfsiz).
  - Imzo mos kelmasa, `ForbiddenException("Webhook imzosi noto'g'ri")` qaytariladi.
- **Xulosa:** GitHub webhook imzo tekshiruvi eng yuqori kriptografik standartda amalga oshirilgan.

### 2.3. TelegramWebhookController (Qo'shimcha tahlil)
- **Fail-closed tekshiruvi (`TelegramWebhookController.java:41-48`):** 
  Hozirgi mantiq `expectedSecret` mavjud bo'lsagina tokenni tekshiradi. Agar `telegram.bot.webhook-secret` env o'zgaruvchisi ko'rsatilmagan (bo'sh) bo'lsa, so'rov **o'tib ketadi**. Bu **xavfli** (fail-open) holat.
  - **Tavsiya:** Mantiqni fail-closed variantiga o'zgartirish (ya'ni secret token bo'lmasa yoki umuman sozlanmagan bo'lsa `401 Unauthorized` yoki `503 Service Unavailable` qaytarish).
  - **Constant-time taqqoslash:** `expectedSecret.trim().equals(secretToken.trim())` timing-attack uchun zaif. Buni `MessageDigest.isEqual(expectedSecret.trim().getBytes(), secretToken.trim().getBytes())` ga o'zgartirish taklif etiladi.
  - **Render env:** Render muhitida albatta `telegram.bot.webhook-secret` qiymatini o'rnatish shart qilib belgilanishi kerak.

---

## 3. WorkspaceInvitationService (Email va holat tekshiruvlari)

### 3.1. Email orqali taklif qabul qilish (`WorkspaceInvitationService.java:122-141`)
- **Email tasdiqlanganligi:** Tizimda (`UserService` yoki ro'yxatdan o'tishda) email verification (tasdiqlash) mexanizmi hozircha topilmadi (`isEmailVerified` tekshiruvi mavjud emas). Shu sababli, agar xaker boshqa birovning email manzili bilan tizimdan ro'yxatdan o'tsa, o'sha emailga kelgan barcha takliflarni qabul qila olish xavfi mavjud.
- **Tavsiya:** `User` modeliga `emailVerified` maydonini qo'shish va ro'yxatdan o'tishda OTP yoki link orqali tasdiqlashni joriy qilish.

### 3.2. PENDING va Muddat (Expiry) tekshiruvi
- **Holat tekshiruvi:** `acceptInvitation` va `rejectInvitation` metodlarida taklif holati aniq `PENDING` ekanligi tekshiriladi: `invitation.getStatus() != InvitationStatus.PENDING` (`WorkspaceInvitationService.java:127`).
- **Muddat (Expiry):** Taklif muddati o'tganligi tekshiriladi: `invitation.getExpiresAt().isBefore(LocalDateTime.now())` (`WorkspaceInvitationService.java:127`).

### 3.3. Ikki marta qabul qilish (Race Condition)
- **Tahlil:** `acceptInvitation` metodi `@Transactional` bilan himoyalangan, biroq `InvitationStatus.PENDING` tekshiruvida bazaga Pessimistic Lock qo'yilmagan (masalan, `FOR UPDATE` bilan o'qish). Bu narsa bir vaqtning o'zida ikkita parallel so'rov kelganida (race condition) ikkita `WorkspaceMember` yozuvini yaratib yuborish xavfini tug'diradi.
- **Tavsiya:** Repozitoriydagi `findById` metodiga `@Lock(LockModeType.PESSIMISTIC_WRITE)` qo'shish yoki Optimistic Locking (`@Version`) ishlatish.

---

## 4. IDOR Audit (Insecure Direct Object Reference)

Quyida barcha ko'rsatilgan Controller'larning resurslarga yetishishdagi avtorizatsiya (IDOR) holati tekshiruvi:

| Endpoint (Controller) | Resurs egaligi/a'zolik tekshiriladimi | Qaysi servis metodida (fayl:qator) | Xulosa |
|-----------------------|---------------------------------------|------------------------------------|--------|
| **TaskController** | Ha | `TaskService.java:91, 126, 237, 259` | **Xavfsiz**. Har bir zaprosda currentUser'ning workspace'ga a'zoligi (`authorizationService.checkAccess/checkCanEdit`) tekshiriladi. |
| **TaskDirectController**| Ha | `TaskService.java:250` | **Xavfsiz**. ID orqali bevosita olganda ham task joylashgan `workspaceId` olinib, a'zolik tekshiriladi (`checkAccess(task.getWorkspaceId(), currentUser)`). |
| **AttachmentController**| Ha | `AttachmentService.java:41, 62, 76` | **Xavfsiz**. Yuklash, ko'rish va o'chirishdan oldin taskning workspace'iga ruxsat tekshiriladi. |
| **CommentController** | Ha | `CommentService.java:45, 54, 78, 108` | **Xavfsiz**. Izoh qo'shish, ko'rish, tahrirlashda `authorizationService.checkAccess(workspaceId, currentUser)` chaqiriladi. |
| **TaskChecklistController** | Ha | `TaskChecklistService.java:36` | **Xavfsiz**. `authorizationService.checkCanEdit` va `checkAccess` yordamida himoyalangan. |
| **FileController** (`/api/files/{fileName}`) | **YO'Q (Katta ehtimol bilan)** | `FileController.java:40` | **Zaiflik (IDOR)**. Endpoint ochiq. Kimdir fayl nomini bilsa (yoki URL ni topsa), authentication principal va workspace a'zoligi tekshirilmasdan bevosita `FileStorageService.loadFileAsResource` chaqiriladi. Buni auth-guard bilan yopish kerak. |

