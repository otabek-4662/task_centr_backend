# Test Triage Hisoboti (com.taskcenter.robustness)

Ushbu hujjat `com.taskcenter.robustness` paketidagi 20 ta integratsion testlarning tahlili, dastlabki va joriy nosozliklar sababi hamda ilovaning haqiqiy xatolari (HAQIQIY XATO) bo'yicha xulosalarni o'z ichiga oladi.

---

## 1. Barcha testlar holati jadvali

| № | Test sinfi | Test metodi | Holati | Xato / Tahlil turi | Tavsif |
|---|---|---|---|---|---|
| 1 | `InitDataRobustnessTest` | `missingHash_producesRejectionAndNotException` | **PASSED** | — | Hash mavjud bo'lmaganda 401 qaytaradi |
| 2 | `InitDataRobustnessTest` | `duplicateKeys_producesRejectionAndNotException` | **PASSED** | — | Takrorlangan kalitlar uzatilganda 401 qaytaradi |
| 3 | `InitDataRobustnessTest` | `malformedUserJson_producesRejectionAndNotException` | **PASSED** | — | Buzilgan user JSON da istisnosiz 401 qaytaradi |
| 4 | `InitDataRobustnessTest` | `expiredAuthDate_producesRejectionAndNotException` | **PASSED** | — | Muddati o'tgan `auth_date` da 401 qaytaradi |
| 5 | `InitDataRobustnessTest` | `emptyBotToken_producesRejectionAndNotException` | **PASSED** | — | Bo'sh bot tokeni bo'lganda validatsiya rad etiladi |
| 6 | `JwtRobustnessTest` | `expiredToken_isRejected` | **PASSED** | — | Muddati o'tgan JWT 403 bilan rad etiladi |
| 7 | `JwtRobustnessTest` | `tokenWithWrongKey_isRejected` | **PASSED** | — | Noto'g'ri kalit bilan imzolangan JWT 403 bilan rad etiladi |
| 8 | `JwtRobustnessTest` | `tokenForDeletedUser_isRejected` | **PASSED** | — | O'chirilgan foydalanuvchi tokeni 403 bilan rad etiladi |
| 9 | `TelegramLinkingRobustnessTest` | `sameTelegramIdCannotBeLinkedToTwoAccounts` | **PASSED** | TEST XATOSI (tuzatildi) | Bot servisi `@SpyBean` qilindi, `entityManager.flush()/clear()` qo'shildi |
| 10 | `TelegramLinkingRobustnessTest` | `relinkingSameAccountIsIdempotent` | **PASSED** | TEST XATOSI (tuzatildi) | Mockito NotAMockException xatosi `@SpyBean` orqali tuzatildi |
| 11 | `TelegramLinkingRobustnessTest` | `linkTokenExpires` | **PASSED** | TEST XATOSI (tuzatildi) | Mockito NotAMockException xatosi `@SpyBean` orqali tuzatildi |
| 12 | `TelegramLinkingRobustnessTest` | `linkTokenCannotBeUsedTwice` | **PASSED** | TEST XATOSI (tuzatildi) | Mockito NotAMockException xatosi `@SpyBean` orqali tuzatildi |
| 13 | `TelegramLinkingRobustnessTest` | `linkTokenStopsWorkingAfterUnlink` | **PASSED** | TEST XATOSI (tuzatildi) | Mockito NotAMockException xatosi `@SpyBean` orqali tuzatildi |
| 14 | `WorkspaceIsolationRobustnessTest` | `userCannotReadTaskFromOtherWorkspace` | **PASSED** | TEST XATOSI (tuzatildi) | `createTestUser` dagi kutilgan 200 o'rniga 201 o'rnatildi |
| 15 | `WorkspaceIsolationRobustnessTest` | `userCannotEditTaskFromOtherWorkspaceEvenWithFakeUrl` | **PASSED** | TEST XATOSI (tuzatildi) | Setupdagi 200 o'rniga 201 o'rnatildi |
| 16 | `WorkspaceIsolationRobustnessTest` | `userCannotChangeColumnFromOtherWorkspace` | **PASSED** | TEST XATOSI (tuzatildi) | Setupdagi 200 o'rniga 201 o'rnatildi |
| 17 | `WorkspaceIsolationRobustnessTest` | `plainMemberCannotDeleteColumn` | **PASSED** | TEST XATOSI (tuzatildi) | A'zo taklif qilishda `name` o'rniga `usernameOrEmail` maydoni kiritildi |
| 18 | `WorkspaceIsolationRobustnessTest` | `removedUserLosesAccess` | **FAILED** | TEST XATOSI (tuzatildi) | Setupda `GET /api/users/me` noto'g'ri URL ishlatilgan (haqiqiy endpoint: `GET /api/me`), DTO maydoni tuzatildi |
| 19 | `SensitiveDataRobustnessTest` | `otherUsersDoNotLeakSensitiveData_inWorkspaceMembers` | **FAILED** | **HAQIQIY XATO** | Oddiy foydalanuvchiga `GET /api/users?workspaceId=...` ga kirish `SecurityConfig` da bloklangan (403) |
| 20 | `SensitiveDataRobustnessTest` | `otherUsersDoNotLeakSensitiveData_inTaskAssignees` | **FAILED** | TEST XATOSI (tuzatildi) | Setupda `GET /api/users/me` noto'g'ri URL va assign endpointi tuzatildi |

---

## 2. Haqiqiy xatolar (HAQIQIY XATO) tahlili

### 1-topilma: Workspace a'zolari va egasi uchun `/api/users?workspaceId={id}` endpointiga kirishning `SecurityConfig` tomonidan taqiqlanishi (403 Forbidden)

- **Test nomi:** `SensitiveDataRobustnessTest.otherUsersDoNotLeakSensitiveData_inWorkspaceMembers`
- **Aniq so'rov (Exact Request):**
  - **Method:** `GET`
  - **URL:** `/api/users?workspaceId={wsId}`
  - **Headers:** `Authorization: Bearer <sens_user_a_token>` (roli oddiy `USER`, ushbu workspace egasi)
  - **Body:** mavjud emas
- **Kutilgan natija (Expected Result):**
  - `200 OK` — Foydalanuvchi o'z workspace'idagi foydalanuvchilar ro'yxatini (`Page<UserDto>`) olishi, unda boshqa foydalanuvchilarning `password`, `telegramLinkToken`, `telegramChatId` kabi maxfiy ma'lumotlari oshkor bo'lmasligi kerak.
- **Haqiqiy natija (Actual Result):**
  - `403 Forbidden` — `{"success":false,"message":"Kirish taqiqlangan","data":null}`
- **Sababchi kod (src/main):**
  - Fayl va qator: [SecurityConfig.java:133](file:///c:/Users/Bekmurod/Desktop/task_center_backend/src/main/java/com/taskcenter/config/SecurityConfig.java#L133)
  ```java
  // Admin only endpoints
  .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
  ```
- **Xato mohiyati va tahlili:**
  `UserController.java` (51-58 qatorlar) da `GET /api/users` metodi workspace foydalanuvchilari ro'yxatini olish uchun mo'ljallangan va `UserService.java` (87-qator) da `authorizationService.checkAccess(workspaceId, currentUser)` orqali tekshiriladi (ya'ni joriy foydalanuvchi mazkur workspacening a'zosi yoki egasi bo'lishi kifoya). Ammo `SecurityConfig.java` dagi 133-qatorda butun `/api/users` endpointi global `ADMIN` roliga cheklangan. Natijada oddiy `USER` rolidagi workspace egasi yoki a'zosi ushbu endpointni chaqira olmaydi va so'rov Security filter zanjiridayoq 403 bilan to'xtatiladi.

---

## 3. Test xatolari va tuzatishlar tavsifi

1. **`createTestUser` status kodi (200 vs 201):**
   `AuthController.registerUser` muvaffaqiyatli ro'yxatdan o'tishda `201 Created` qaytaradi. Testlarda `status().isOk()` (200) kutilgani sababli barcha test setup'lari dastlabki qadamdayoq to'xtab qolgan edi.

2. **`WorkspaceMemberInviteRequest` validatsiya maydoni (`name` vs `usernameOrEmail`):**
   `POST /api/workspaces/{workspaceId}/members` endpointi `WorkspaceMemberInviteRequest` DTO sini qabul qiladi. Unda `@NotBlank private String usernameOrEmail;` talab qilinadi. Testlar `{"name": "...", "role": "MEMBER"}` yuborayotgan edi, bu esa 400 Bad Request validatsiya xatosini keltirib chiqarayotgan edi.

3. **Foydalanuvchi ma'lumotlarini olish URL i (`GET /api/users/me` vs `GET /api/me`):**
   Ilovada joriy foydalanuvchi profilini olish endpointi `GET /api/me` (va `GET /api/auth/me`) hisoblanadi. `/api/users/me` faqat `PATCH` metodini qo'llab-quvvatlaydi. Testlarda setup sifatida `GET /api/users/me` chaqirilishi `HttpRequestMethodNotSupportedException` (500) ga olib kelayotgan edi.

4. **Vazifaga ijrochi biriktirish endpointi URL va statusi:**
   `POST /api/workspaces/{wsId}/tasks/{taskId}/assignees` endpointi mavjud emas edi. Haqiqiy endpoint `POST /api/workspaces/{wsId}/tasks/{taskId}/assign?userId={userId}` bo'lib, `200 OK` qaytaradi.

5. **Telegram bot testlaridagi `@Autowired` vs `@SpyBean` va DB sinxronizatsiyasi:**
   `TelegramLinkingRobustnessTest` da `TelegramBotService` mock bo'lmagan holda `Mockito.when()` chaqirilgan edi, bu `NotAMockException` keltirib chiqardi. `@SpyBean` ga o'tkazildi hamda real bazadagi holatni to'g'ri o'qish uchun `entityManager.flush()` va `entityManager.clear()` chaqirilib, xotiradagi kesh emas, balki tranzaksiyadan so'ng bazaga yozilgan holat tekshirilishi ta'minlandi.
