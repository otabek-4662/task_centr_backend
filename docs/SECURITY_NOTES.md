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
