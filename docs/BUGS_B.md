# Telegram Bildirishnomalar va Scheduler (QISM B) Audit Hisoboti

## 1. Missed sends (Uyqudagi server)
- **Daraja:** Critical
- **Fayl:** `TelegramSchedulerService.java`:77, 85
- **Muammo:** Render FREE tarifidagi serverlar 15 daqiqa harakatsizlikdan so'ng uyquga ketadi. `@Scheduled` vazifalar (08:00 digest, 18:00 reminders) server uxlayotgan vaqtda ishlamaydi va ularni o'tkazib yuborilganini tekshirib qayta ishga tushiradigan mexanizm yo'q. Natijada foydalanuvchilarga xabarlar bormaydi.
- **Qayta hosil qilish:** Serverni uyquga ketishini kuting (Render'da). Soat 08:05 da saytga kirib uni uyg'oting, lekin kunlik hisobot kelmaydi.
- **Minimal yechim:** `cron-job.org` kabi tashqi pinger orqali `/health` yoki `/ping` endpoint'ini har kuni 07:55 va 17:55 da chaqirib serverni uyg'oq tutish kerak. Yoki `ApplicationReadyEvent` da oxirgi marta xabar yuborilgan vaqtni tekshirib, missed job'larni ishga tushirish kerak.

## 2. Duplicate sends (Ikki marta yuborish)
- **Daraja:** Critical
- **Fayl:** `TelegramSchedulerService.java`:85
- **Muammo:** Kunlik digest yuborish mexanizmida holat (status) ma'lumotlar bazasida saqlanmaydi va `@SchedulerLock` kabi taqsimlangan qulf (distributed lock) ishlatilmagan. Agar server deploy paytida 2 ta instance (nusxa) bir vaqtda ishlasa, barcha foydalanuvchilarga 2 martadan hisobot yuboriladi.
- **Qayta hosil qilish:** Ilovaning 2 ta nusxasini bir vaqtda ishga tushiring, soat 08:00 da foydalanuvchilar bitta xabarni ikki marta oladi.
- **Minimal yechim:** `@SchedulerLock` (ShedLock) orqali metodni qulflash yoki `telegram_digest_log` jadvali yaratib, yuborishdan oldin bazaga "yuborildi" flagini yozish kerak.

## 3. Telegram Rate Limits (429 xatoligi va Thundering Herd)
- **Daraja:** High
- **Fayl:** `TelegramBotService.java`:1191 (executeWithRetry metodi)
- **Muammo:** Digest va Reminder jo'natish tsiklida 100-1000 ta xabar bir vaqtning o'zida asinxron thread'larga beriladi. Telegram bot API'da ~30 ta xabar/soniya limiti bor. Katta hajmda limitdan oshganda, Telegram `429 Too Many Requests` qaytaradi. Kod faqat bir marta (`attempt == 0`) xuddi shu concurrent tezlikda qayta urinadi va ko'pchilik xabarlar butunlay yo'qotiladi.
- **Qayta hosil qilish:** 100+ ta botga ulangan test userlarga kunlik hisobot yuboring, loglarda 429 xatoligi chiqadi va ba'zi foydalanuvchilar xabar olmaydi.
- **Minimal yechim:** Xabarlarni yuborishdan oldin limit qo'yuvchi mexanizm qo'shing (masalan, Google Guava `RateLimiter.create(25.0)`) yoki navbat (queue) orqali har xabar orasida 40ms pauza (`Thread.sleep`) qiling.

## 4. N+1 Performance & Transaction Bug
- **Daraja:** High
- **Fayl:** `TelegramSchedulerService.java`:193 va 199
- **Muammo:** `sendDueTomorrowReminders` metodida, har bir foydalanuvchining har bir vazifasi uchun sikl ichida `reminderLogRepository.existsBy...` chaqirilgan (N+1 query). Bundan tashqari, `@Scheduled` metodida `@Transactional` yo'q, ya'ni loglar darhol alohida commit bo'ladi. Agar xabar yuborish (rate limit tufayli) o'xshamasa, baza allaqachon "yuborildi" deb belgilab qo'ygan bo'ladi.
- **Qayta hosil qilish:** Bitta userga 50 ta vazifa biriktiring va schedulerni ishlating. Bazaga 50 ta alohida SELECT so'rov tushadi. Telegram o'chirilgan bo'lsa ham loglar yozilib qoladi.
- **Minimal yechim:** Barcha loglarni sikldan tashqarida `taskId IN (...)` bilan bir marta o'qing va metodga `@Transactional` annotatsiyasini qo'shing.

## 5. Timezone (Mintaqa) xatoligi
- **Daraja:** Medium
- **Fayl:** `TelegramSchedulerService.java`:336 va `TelegramNotificationService.java`:98
- **Muammo:** Sokin soatlar (quiet hours) `LocalTime.now(clock)` orqali tekshiriladi. Serverdagi `clock` UTC bo'ladi, lekin foydalanuvchilar (Asia/Tashkent) o'z mahalliy vaqti bilan sokin soatni kiritgan. Masalan, 23:00 (Toshkent) da UTC vaqti 18:00 bo'ladi, kod 18:00 ni sokin soatga kirmaydi deb hisoblab, yarm tunda xabar yuborib yuboradi.
- **Qayta hosil qilish:** Userning sokin soatini 22:00-08:00 qiling, server UTC da bo'lsin. Toshkent vaqti bilan 23:00 da comment yozing, xabar yetib boradi (aslida bormasligi kerak).
- **Minimal yechim:** `LocalTime.now(clock.withZone(ZoneId.of("Asia/Tashkent")))` qilib vaqtni aniq belgilash kerak.

## 6. Xabarlar mazmuni sizib chiqishi (Data Leak)
- **Daraja:** Medium
- **Fayl:** `TelegramNotificationService.java`:82
- **Muammo:** Task bo'yicha izoh yozilganda, assignees (biriktirilganlar) va watchers (kuzatuvchilar) ga xabar ketadi. Agar biror foydalanuvchi workspace'dan o'chirilgan bo'lsa, lekin hali ham task assignee sifatida qolib ketgan bo'lsa, unga ham xabar (izoh matni bilan) ketaveradi, chunki ularni `isWorkspaceMember` orqali tekshirilmagan (mentions'dan farqli ravishda).
- **Qayta hosil qilish:** User A ni Task'ga assignee qiling, so'ng User A ni Workspace'dan o'chirib tashlang. Boshqa odam Task'ga izoh yozsa, User A Telegram orqali shu izohni o'qiy oladi.
- **Minimal yechim:** `sendIfEligible` metodida yoki yig'ish jarayonida foydalanuvchi hozirda workspace a'zosi ekanligini albatta tekshirish.

## 7. Xabar uzunligi chegarasi (4096 belgi)
- **Daraja:** Low
- **Fayl:** `TelegramSchedulerService.java`:217
- **Muammo:** `sendDueTomorrowReminders` da tasklar nomlari `StringBuilder` ga qo'shilib boradi, lekin Telegram'ning 4096 belgili chegarasidan oshib ketmasligi tekshirilmagan (digest'da tekshirilgan). Ko'p taskli userning xabari 400 Bad Request bilan qaytadi va umuman eslatma olmaydi.
- **Qayta hosil qilish:** Bitta userga yuzlab uzun nomli tasklarni ertangi kunga biriktiring va eslatmani ishlating, xabar ketmaydi.
- **Minimal yechim:** Uzunlik 4000 belgidan oshganda tsiklni to'xtatib, "... va yana N ta" deb qisqartirish.

## 8. Noto'g'ri test yozilishi (Mocking Time)
- **Daraja:** Low
- **Fayl:** `TelegramSchedulerServiceTest.java`:47
- **Muammo:** Schedulerni sinashda `setUp` metodida `Clock.systemDefaultZone()` berilgan. Testlarda esa `now` (vaqt) parametr sifatida to'g'ridan-to'g'ri ichki metodlarga beriladi. Bu `@Scheduled` dagi timezone qanday ishlayotganini yoki `LocalDateTime.now(clock)` UTC da ishlasa nima bo'lishini umuman test qilmaydi (assert nothing on real time behaviour).
- **Qayta hosil qilish:** Testlarni ishga tushiring, ular o'tadi, lekin aslidagi buglarni topa olmaydi.
- **Minimal yechim:** Spring Context bilan test yozish, yoki Clock'ni har xil timezone'larda berib `scheduleDueTomorrowReminders()` (parametrsiz) metodini to'g'ridan-to'g'ri sinash.

---

### Eng birinchi tuzatish kerak bo'lgan 5 ta:
1. Missed sends (Render'da uyquga ketgan paytda digest yo'qotilishi)
2. Duplicate sends (Ko'p nusxali ishlashda ikki marta yuborish muammosi)
3. Telegram Rate Limits (429 xatosi va Thundering herd - xabarlar blokka tushishi)
4. N+1 Performance & Transaction Bug (Loglarni tsikl ichida birma-bir tekshirish)
5. Data Leak (Workspace'dan o'chirilgan a'zolarga ma'lumotlar ketib qolishi)

### Tekshirib bo'lmaganlar:
- **Shubhali:** `TelegramBotService` da asinxron `handleTelegramMessageEvent` ga xabarlar yuborilishi haqida so'rovlar yetib boradi. Agar API bilan 403 Forbidden qilib foydalanuvchilar o'chirilsa, ularning aloqasini uzish (`userRepository.save(u)`) tranzaksiya ichida muammosiz bajariladimi yo'qmi haqiqiy muhitda kuzatish kerak. Shuningdek, `WebSocketNotifier.notifyWorkspace` amallari ba'zi joylarda bazaga yozish bilan bir xil tranzaksiya ichida chaqirilgan. Agar WebSocket ulanishida kutilmagan Runtime xatolik yuzaga kelsa, xabarni deb butun tranzaksiya (masalan, task yaratish) bekor bo'lib qolishi mumkin. Buni tekshirish lozim.
