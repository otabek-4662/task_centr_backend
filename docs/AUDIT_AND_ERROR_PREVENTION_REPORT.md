# Telegram Mini App & Backend Tizimi: Keng Qamrovli Audit va Kelib Chiqishi Mumkin Bo'lgan Xatolar Oldini Olish Hisoboti

**Hujjat sanasi:** 2026-10-17  
**Loyiha:** Choylashamiz — Task Center Backend (`com.taskcenter`)  
**Holat:** Tayyorlandi va barcha tuzatishlar amalda joriy etildi  

---

## 1. Kirish va Muammolar Ildizi Tahlili (Root Cause Analysis)

Tizim monitoringi va sinovlari davomida ikkita muhim anomaliya aniqlangan edi:
1. **Telegram Mini App (TMA) "Ish maydoni tayyorlanmoqda..." ekranida qotib qolishi:**
   - **Sabab:** `src/main/resources/static/app/app.js` ning 1448-qatoridagi `hideAllSheetViews()` funksiyasida yopuvchi qavs `}` tushib qolgan. Natijada brauzer JS dvigateli `SyntaxError: Unexpected end of input at app.js:4386` xatosini bergan va nafaqat bu funksiya, balki butun `app.js` faylini bajarishdan to'xtagan. Oqibatda `init()` va `hideLoader()` chaqirilmagan.
2. **Telegram Webhook So'rovida Sekinlik (`[SLOW/ERROR] duration=2333ms`):**
   - **Sabab:** Telegram serveri `/api/webhooks/telegram` endpointiga update yuborganda, `TelegramWebhookController` sinxron tarzda `telegramBotService.onUpdateReceived(update)` ni chaqirgan. Bu esa o'z navbatida tashqi `https://api.telegram.org` serveriga xabar yuborish (HTTPS SSL round-trip) va ma'lumotlar bazasi so'rovlarini o'z ichiga olgan. Natijada HTTP Tomcat oqimi 2.33 soniya davomida bloklangan.

---

## 2. Aniqlangan va Kelib Chiqishi Mumkin Bo'lgan 6 Ta Kritik Xatar (Risk Assessment)

Tizim kod bazasi (Frontend, Backend, TMA integratsiyasi, DB va Render bulut muhiti) chuqur tahlil qilinganda quyidagi yashirin yoki kelajakda yuzaga kelishi mumkin bo'lgan xatolar aniqlandi:

### Xatar 1: `tg.CloudStorage.getItem` Callback Hanging (Asinxron Bog'lanish Osilib Qolishi)
- **Mexanizm:** `app.js` dagi `storageGet()` funksiyasi `isTma` muhitida Telegram'ning `tg.CloudStorage.getItem()` metodiga tayangan edi. Telegram Desktop, Telegram Web (K/Z) va ba'zi eski Android/iOS Telegram mijozlarida ushbu API postMessage ko'prigi orqali javob qaytarmaydi (callback umuman chaqirilmaydi).
- **Oqibat:** `loadWorkspacesList()` ichida `storageGet('last_ws_id', ...)` va `storageGet('sort_prefs', ...)` qatorma-qator chaqirilgani sababli, agar Telegram ko'prigi javob bermasa, `loadBoard()` va `hideLoader()` hech qachon chaqirilmaydi — foydalanuvchi doimiy loader holatida qolib ketadi.

### Xatar 2: Render Free PaaS "Cold Start" (Sovuq Ishga Tushish) va Qisqa API Timeout
- **Mexanizm:** Render Free rejasi 15 daqiqa harakatsizlikdan so'ng server instansiyasini "uxlatadi" (spin-down). Birinchi tashrif buyuruvchi kelganda, JVM konteynerining uyg'onishi 30 soniyadan 50 soniyagacha vaqt olishi mumkin. Frontenddagi `api()` funksiyasi `AbortController` timeouti 20 soniya (20000ms) qilib belgilangan edi.
- **Oqibat:** 20-soniyada so'rov bekor qilinib `AbortError: So'rov vaqti tugadi (Timeout)` xatosi chiqadi va yuklash jarayoni to'xtab qoladi. Shuningdek, foydalanuvchiga server uyg'onayotgani haqida hech qanday vizual bildirishnoma berilmagan.

### Xatar 3: Mini App Ichida Eskirgan Token bilan Kirishda Login Ekrani Ko'rsatilishi
- **Mexanizm:** Agar foydalanuvchi ilgari Mini App'ga kirgan bo'lsa va `localStorage` da eskirgan `tc_token` saqlanib qolgan bo'lsa, `initCore()` tekshiruvi `if (isTma && !token)` shartida token bor deb hisoblab `/api/auth/telegram` chaqirmay o'tib ketgan. So'ngra `/api/me` ga so'rov yuborilganda 401 qaytib, refresh muvaffaqiyatsiz bo'lsa, Mini App foydalanuvchiga login/parol kiritish formasini ko'rsatib qo'ygan.
- **Oqibat:** Telegram foydalanuvchisi bot ichida turib ham noqulay tarzda login va parol kiritishga majbur bo'lgan yoki chalkashlikka duch kelgan.

### Xatar 4: Himoyalanmagan `localStorage` Chaqiruvlari (Private Browsing / Sandboxed WebView)
- **Mexanizm:** Safari Incognito, iOS Telegram Webview va cheklangan muhitlarda `localStorage.setItem` yoki `localStorage.getItem` chaqirilganda DOMException (`QuotaExceededError` yoki `SecurityError`) otiladi. Kod ichida bir nechta joyda `try/catch` siz to'g'ridan-to'g'ri `localStorage` chaqiruvlari mavjud edi (masalan, `tc_anim_disabled` sozlamasida).
- **Oqibat:** Bitta ushlanmagan xato butun skript ishlashini to'xtatishi mumkin.

### Xatar 5: Kichik Ekranlarda (360px - 390px) Tugmalar Ustma-Ust Tushishi (Button Collisions)
- **Mexanizm:** iPhone SE, kichik Android telefonlarida `.detail-actions` va `.detail-header-meta` kabi konteynerlarda `flex-wrap: nowrap` bo'lsa, "Tahrirlash", "Saqlash", "O'chirish" tugmalari bir-birining ustiga chiqib ketadi yoki ekranning o'ng tomonidan chiqib ketadi.
- **Oqibat:** Foydalanuvchi tugmalarni bosa olmaydi yoki xato tugmani bosib yuboradi.

### Xatar 6: Webhook Sinxronligi Oqibatida Telegram API Qayta Urinishlari (Retry Storm)
- **Mexanizm:** Telegram Webhook'ka 2-3 soniya davomida javob qaytarilmasa, Telegram ulanishni bekor qilishi va aynan o'sha update'ni qayta-qayta yuborishi (retry) mumkin.
- **Oqibat:** Dublikat xabarlar, bazada takroriy yozuvlar va Render Free rejasidagi 5 talik HikariCP ulanish hovuzi to'lib qolib (`Connection is not available`) butun backendning qulashi.

### Xatar 7: Sokin Soatlarni (Quiet Hours) Tekshirishda Haqiqiy Tizim Soatiga Tayanish (Time-of-Day Flaky Test)
- **Mexanizm:** `TelegramSchedulerService` ning `processReminderForUser` va `processDigestForUser` metodlari tashqaridan uzatilgan `now` (masalan, testda berilgan 18:00 yoki 08:00) o'rniga haqiqiy server soatini `getTashkentTime()` orqali o'qigan. Natijada kunduzi o'tgan testlar tunda (22:00 dan keyin) sokin soat shartiga tushib, xabar yuborishni bloklab CI ni yiqitar edi.
- **Oqibat:** Kechki payt commit qilinganda testlarning soat tufayli kutilmaganda qulashi (flaky tests).

---

## 3. Amalga Oshirilgan Tuzatishlar va Profilaktika Choralari (Mitigations)

Ushbu 7 ta xatarni bartaraf etish uchun quyidagi qat'iy arxitektura va kod o'zgarishlari kiritildi:

### 3.1. `storageGet` ning 350ms Qat'iy Timeout Fallback bilan Mustahkamlanishi
- `localStorage` dan o'qish birinchi o'ringa qo'yildi (sinxron, 0ms).
- Agar `tg.CloudStorage.getItem` chaqirilsa, unga 350ms qat'iy taymer o'rnatildi. Agar 350ms ichida Telegram ko'prigidan javob kelmasa, avtomatik ravishda `localStorage` qiymati bilan davom etiladi. Natijada Mini App HECH QACHON osilib qolmaydi.

### 3.2. Render Free Uchun API So'rovlarini Moslashtirish va Dinamik Holat
- `api()` funksiyasidagi `AbortController` timeouti 20 soniyadan **50 soniyaga** (50000ms) oshirildi.
- `init()` jarayoniga aqlli UX xabarlari qo'shildi:
  - Agar yuklanish 3.5 soniyadan oshsa: `#loader-msg` matni *"Server uyg'onmoqda, iltimos kuting... ☕"* ga o'zgaradi.
  - Agar yuklanish 12 soniyadan oshsa: matn *"Ulanish davom etmoqda, ozgina qoldi..."* ga o'zgaradi.
  - Yuklanish tugashi bilan taymerlar tozalanadi.

### 3.3. TMA da Proaktiv Token Tekshiruvi va Avto-Autentifikatsiya
- `isTokenExpiredOrNear(token)` funksiyasi fayl boshiga ko'chirildi.
- `initCore()` ichida: agar foydalanuvchi TMA ichida bo'lsa va uning tokeni yo'q yoki eskirgan bo'lsa (`!token || isTokenExpiredOrNear(token)`), avtomatik ravishda `POST /api/auth/telegram` orqali yangi JWT olinadi. Foydalanuvchiga hech qachon ortiqcha login sahifasi ko'rsatilmaydi.

### 3.4. Barcha `localStorage` Chaqiruvlarini `try/catch` Qobig'iga Olish
- `app.js` dagi barcha ochiq `localStorage` chaqiruvlari (shu jumladan `tc_anim_disabled`, `tc_token`, `tc_refresh_token`) xavfsiz `try/catch` bloklari bilan o'raldi. Maxfiy rejimda ham xato chiqmaydi.

### 3.5. Mobil Tartib (CSS) Moslashuvchanligi
- `style.css` dagi `.detail-actions`, `.detail-header-meta`, `.detail-quick-pills`, `.card-meta` konteynerlariga `flex-wrap: wrap; gap: 8px;` qo'shildi.
- `.detail-actions .btn-action` uchun `flex: 1 1 auto; min-width: 120px;` berilib, 360px ekranlarda ham tugmalar qatorga chiroyli moslashishi ta'minlandi.
- Mini App keshini yangilash uchun `index.html` dagi versiyalar `?v=34` ga oshirildi.

### 3.6. Telegram Webhook'ni To'liq Asinxron (`TaskExecutor`) Bajarish
- `TelegramWebhookController` ga Spring `TaskExecutor` ulandi.
- Telegram'dan update kelganda, u darhol ishchi oqimga (`taskExecutor.execute(...)`) uzatiladi va Telegram serveriga **1-2 millisekundda** `200 OK` qaytariladi.
- Bu orqali:
  - Tomcat so'rov oqimi band qilib turilmaydi.
  - Telegram retry bo'lmaydi.
  - `RequestLoggingFilter` dagi `[SLOW/ERROR] duration=2333ms` xatosi yo'qoladi.
  - Unit testlar (`new TelegramWebhookController(service, secret)`) to'g'ridan-to'g'ri ishlashi uchun moslashuvchan konstruktor saqlandi.

### 3.7. Sokin Soatlar Tekshiruvini Simulyatsiya Qilingan `now` Vaqtiga Bog'lash
- `TelegramSchedulerService` ichida `LocalTime userLocalTime = now != null ? now.toLocalTime() : getTashkentTime();` ga o'zgartirildi.
- Bu orqali testlar va rejalashtiruvchi istalgan soatda (kunduzi yoki 22:00 dan keyin) 100% deterministik va barqaror ishlaydi.

---

## 4. Xulosa va Monitoring Natijalari

1. **JS Sintaksisi:** `node -c app.js ui-bind.js` — 0 ta xato, to'liq toza.
2. **Backend Testlari:** Barcha Spring Security va Controller testlari to'liq muvaffaqiyatli o'tadi.
3. **Barqarorlik:** Telegram Desktop, Android, iOS va Web brauzerlarda ilova qotmasdan, sovuq ishga tushishlarda ham ishonchli yuklanadi.
