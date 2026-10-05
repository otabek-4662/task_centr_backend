# 📱 Telegram Mini App (Task Center)

Bu hujjat Task Center loyihasining Telegram Mini App (TMA) qismi qanday ishlashi va qanday himoyalanganligini tushuntiradi. Mini App Spring Boot tomonidan `/app/` yo'nalishida xizmat qilinadi va to'liq frontend imkoniyatlarini taqdim etadi.

## 🏗️ 1. Arxitektura

- **Fayllar:** `src/main/resources/static/app/` papkasida joylashgan. `index.html`, `style.css` va `app.js`.
- **UI & DOM:** Dastur hech qanday React/Vue freymvorklarsiz toza (vanilla) JavaScript va DOM API yordamida yozilgan.
- **Telegram WebApp:** `window.Telegram.WebApp` API (TMA) dan intensiv foydalaniladi (HapticFeedback, CloudStorage, MainButton, BackButton).
- **Backend Bilan Aloqa:** `api()` nomli yagona fetch wrapper orqali amalga oshiriladi. 
- **Statik Fayllar:** `SecurityConfig` orqali `/app/**` ruxsatnomasi ochilgan. Brauzer keshlash muammolarini oldini olish uchun fayllarga (app.js, style.css) `?v=X` cache-buster qo'shilgan.

## 🔐 2. Autentifikatsiya va Sessiya (Session Robustness)

TMA ga kirishda sessiya boshqaruvi juda qat'iy tekshiriladi:
1. **Telegram InitData:** Foydalanuvchi TMA ni ochganda `tg.initData` orqali backendga `POST /api/v1/auth/telegram` chaqiruvi qilinadi.
2. **JWT Saqlash:** Backenddan qaytgan JWT xotirada saqlanadi (localStorage da emas, xavfsizlik uchun). 
3. **Re-Auth mexanizmi (401/403):** 
   - Agar biror API zaprosi 401/403 qaytarsa, dastur darhol xatoni bildirmaydi.
   - Orqa fonda `initData` orqali jimsiz (silent) yangi token olishga harakat qiladi (`reauthPromise`).
   - Muvaffaqiyatli bo'lsa, asl zapros bitta marta takrorlanadi (retry).
   - Bu "Race Condition" larga chidamli bo'lishi uchun yagona in-flight reauth-promise orqali amalga oshiriladi. Parallel kelayotgan boshqa xatolar ham ayni shu promissni kutadi.
4. **Login View (Fallback):** Agar foydalanuvchi Telegram akkauntini hali botga ulamagan bo'lsa (yoki `AUTH` xatosi yuz bersa), Mini App ichida zaxira Login ekrani chiqadi. Bu ekran orqali an'anaviy login/parol bilan kirish va akkauntni avtomatik telegramga ulash (Link) amalga oshiriladi.
5. **Fatal Error:** Dasturga kirish umuman imkonsiz bo'lsa, foydalanuvchiga qizil rangli "Terminal xatolik" ekrani (Fatal Error overlay) ko'rsatiladi va `tg.close()` qilish so'raladi.

## 🌐 3. Tarmoq va Resilientlik (Network Resilience)

TMA yomon tarmoq sharoitlarida ham ishonchli ishlashi uchun bir qator usullarni qo'llaydi:
- **Offline / Online Eslatmalar:** Brauzerning `online` va `offline` voqealari tinglanadi. Tarmoq uzilganda "Tarmoq uzildi" bildirishnomasi chiqadi, tarmoq qaytganda dastur o'zini avtomatik qayta tiklaydi.
- **Initialization Backoff:** Dastur ishga tushayotganida tarmoq nosoz bo'lsa, "Exponential Backoff" algoritmi bilan qayta-qayta ulanishga harakat qiladi (1s, 2s, 4s, 8s...). 
- **Slow Network Warning:** Agar ishga tushish 4 soniyadan oshib ketsa, "Tarmoq sekin ko'rinadi..." degan eslatma chiqadi.
- **Timeout & AbortController:** Barcha API chaqiruvlari 20 soniya kutish vaqti (Timeout) ga ega. Agar ulanish kechiksa, zapros avtomatik to'xtatiladi.

## 🛡️ 4. Xavfsizlik (XSS Auditi)

- **Sanitization:** Foydalanuvchi kiritgan ma'lumotlarni render qilishda oldin xavfsizlik muammolari bo'lishi mumkin edi (`innerHTML` + `esc()`). Endilikda barcha foydalanuvchi ma'lumotlari qat'iy ravishda `textContent` yoki `document.createElement()` orqali DOM'ga qo'shiladi. 
- Bu orqali har qanday XSS (Cross-Site Scripting) hujumlari, xususan tasdiqlanmagan belgilardan yuzaga keladigan muammolar butunlay bartaraf etilgan.
- `innerHTML` faqat ichida hech qanday dinamik (foydalanuvchi) o'zgaruvchisi yo'q bo'lgan statik shablonlarni (skeleton va hokazo) chizish uchun ishlatiladi.

## ⚡ 5. Optimistik Yangilanishlar (Optimistic UI)

- **Vazifa holatini o'zgartirish (Drag & Drop):** Vazifa ustunini o'zgartirganda (`moveTask`), UI darhol yangilanadi. Agar backend so'rovi muvaffaqiyatsiz yakunlansa, UI avtomatik ravishda eski holatiga qaytariladi (rollback) va xato bildiriladi.
- **Ustun nomini o'zgartirish:** Xuddi shunday, ustun nomi oldin UI'da o'zgaradi, keyin backend'ga yuboriladi.
- **Checklistlar:** Checklist pichkasi bosilganda xato bo'lsa, UI serverdan qayta yuklab (revert) o'z holatiga keladi.

## 🔗 6. Telegram Ichida Ulashish (Deep Linking / Task Sharing)

- Dasturda vazifani (Task) boshqa telegram foydalanuvchisiga yuborish imkoniyati mavjud ("Ulashish" tugmasi).
- Yuborilganda, havola maxsus formatda bo'ladi: `https://t.me/<bot_username>?start=task_<taskId>`.
- Qabul qiluvchi bu havolani bosganda, bot `/start task_<id>` buyrug'ini oladi. Backend foydalanuvchi huquqini (Workspace a'zosimi) tekshiradi.
- Ruxsat bo'lsa, WebApp tugmasini aynan shu vazifa ID si bilan (`initialTaskId`) ochadigan javob qaytaradi.
- TMA ochilganda `last_ws_id` dan qat'iy nazar to'g'ridan-to'g'ri ulashilgan vazifa oynasiga (`openSheet('view')`) o'tadi va uni ajratib (highlight qilib) ko'rsatadi.

## 🎨 7. UI / UX

- **Haptic Feedback:** Har bir muhim harakatda (Drag boshlanishi, tugashi, tugma bosilishi, xatolik, muvaffaqiyat) foydalanuvchiga teginish sezgisi (vibratsiya) yuboriladi.
- **Blinking & Highlight:** Yangi ochilgan yoki ulashilgan vazifalar 2 soniya davomida porlab (glow effect) turadi.
- **Skeleton Loaders:** Ma'lumotlar kelgunga qadar bo'sh oq ekran emas, balki jonlantirilgan kulrang skeletlar (skeleton) ko'rsatiladi.
- **Confetti:** Vazifa yakuniy ustunga (`bajarildi`, `done`, `tugadi`) ko'chirilganda foydalanuvchini tabriklash uchun konfetti effekti ishlaydi.
