# Telegram Bot Tahlili Hisoboti (Task Center Backend)

Ushbu hujjat loyihadagi mavjud Telegram bot kodlarini o'rganish va tahlil qilish asosida tayyorlandi.

## 1. FAYLLAR RO'YXATI

*   `src/main/java/com/taskcenter/service/TelegramBotService.java`: Botning asosiy logikasi, kelgan xabarlarni (Update) qabul qilish, buyruqlarni tahlil qilish va inline tugmalarni boshqarish.
*   `src/main/java/com/taskcenter/service/UserService.java` (68-82-qatorlar): Web ilovadan botni ulash uchun kriptografik xavfsiz token yaratish mantiqi.
*   `src/main/java/com/taskcenter/model/User.java` (44-51-qatorlar): Bot bilan ishlash uchun kerakli ustunlar (`telegramChatId`, `telegramLinkToken`, `telegramLinkTokenExpiresAt`).
*   `src/main/java/com/taskcenter/service/TaskService.java` (462-464-qatorlar): Boshqa foydalanuvchiga vazifa biriktirilganda bot orqali avtomatik bildirishnoma yuborish mantiqi.
*   `src/main/resources/db/migration/V35__add_telegram_fields_to_users.sql` va `V41__add_telegram_token_expiry.sql`: Ma'lumotlar bazasida tegishli ustunlarni yaratuvchi migratsiya skriptlari.
*   `pom.xml` (138-143-qatorlar): Telegram bot ishlashi uchun zarur bo'lgan kutubxona.
*   `src/main/resources/application.yml` (82-85-qatorlar): Bot sozlamalari saqlanadigan konfiguratsiya fayli.
*   `src/test/java/com/taskcenter/service/UserServiceTest.java` (144-196-qatorlar): Token yaratish mantiqini tekshiruvchi testlar.

## 2. ISHLASH USULI

*   Bot **Long Polling** usulida ishlaydi (`TelegramBotService` klassi `TelegramLongPollingBot` dan meros olgan, 33-qator).
*   Webhook ishlatilmagan (shuning uchun URL va secret tekshiruvi mavjud emas).
*   Kutubxona: `org.telegram:telegrambots-spring-boot-starter`, versiyasi `6.8.0` (`pom.xml`).

## 3. FOYDALANUVCHINI ULASH (LINKING)

1.  Foydalanuvchi tizim orqali so'rov yuborganda, `UserService.generateTelegramLinkToken()` (71-qator) `SecureRandom` yordamida 8 belgili token yaratib beradi.
2.  Bu token bazadagi `telegram_link_token` va `telegram_link_token_expires_at` (15 daqiqa muddat bilan) maydonlariga saqlanadi (`UserService.java` 78-79-qatorlar).
3.  Foydalanuvchi telegramdan `/start <token>` jo'natadi.
4.  Bot `TelegramBotService.handleStart()` ichida tokenni izlaydi (`userRepository.findByTelegramLinkToken`, 90-qator).
5.  Agar topilsa, foydalanuvchining `telegramChatId` maydoni saqlanadi va token o'chiriladi (`user.setTelegramLinkToken(null)`, 94-qator).
6.  **Unlink (uzish):** Foydalanuvchi botga `/unlink` yuborish orqali chat id'ni `null` qilib, botni tizimdan uzib qo'yishi mumkin (`TelegramBotService.handleUnlink()`, 193-203-qatorlar).

## 4. BOT BUYRUQLARI

| Buyruq | Vazifasi | Kim ishlata oladi | Metod nomi (`TelegramBotService`) | Holati |
| :--- | :--- | :--- | :--- | :--- |
| `/start <token>` | Akkauntni Telegramga ulash | Hamma | `handleStart` | Tayyor |
| `/start` | Xush kelibsiz xabari / yo'riqnoma | Hamma | `handleStart` | Tayyor |
| `/my_tasks` | Foydalanuvchiga biriktirilgan vazifalar | Ulanishdan o'tganlar | `handleMyTasks` | Tayyor |
| `/status` | Foydalanuvchi maqomi / statistika | Ulanishdan o'tganlar | `handleStatus` | Tayyor |
| `/help` | Yordam (qo'llanma) | Hamma | `handleHelp` | Tayyor |
| `/unlink` | Botni tizimdan uzib qo'yish | Ulanishdan o'tganlar | `handleUnlink` | Tayyor |

**Inline tugmalar (Callback Queries):**
*   `TASK_VIEW_<id>`: Vazifaning batafsil ma'lumotini chiqaradi (`handleCallbackQuery`, 236-qator).
*   `TASK_STATUS_SELECT_<id>`: Holatni o'zgartirish uchun loyihadagi ustunlarni chiqaradi (284-qator).
*   `TASK_STATUS_CHANGE_<taskId>_<colId>`: Tanlangan vazifani tegishli ustunga o'tkazadi (319-qator).

## 5. YUBORILADIGAN BILDIRISHNOMALAR

*   **Qaysi hodisada:** Faqat vazifa kimgadir tayinlanganda yuboriladi. Boshqa holatlar (muddat, izoh, va h.k.) mavjud emas.
*   **Qayerdan chaqiriladi:** `TaskService.java` ning 462-464 qatorlaridan.
*   **Sinxronmi:** Ha, bloklovchi (sinxron) usulda chaqirilgan (`@Async` yo'q). 
*   **Xato bo'lsa:** Xato `TelegramBotService` dagi `sendMessage` ichida `log.error` qilinadi, lekin qutqarish/retry mantiqi yo'q (350-352-qatorlar).
*   **Formati va tili:** O'zbek tilida. *"Sizga yangi bosh og'riq (vazifa) biriktirildi! ... Qozonda qaynatish vaqti keldi!"* (`TaskService`, 463-qator).

## 6. MA'LUMOTLAR BAZASI

*   Jadval: `users`
*   Maydonlar: `telegram_chat_id` (BIGINT), `telegram_link_token` (VARCHAR(255)), `telegram_link_token_expires_at` (TIMESTAMP(6)).
*   **Indexlar:** Token va chat_id bo'yicha qidirishni tezlashtiruvchi indexlar bazada va migratsiyalarda topilmadi.

## 7. SOZLAMALAR

*   `application.yml` (82-85): `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME`.
*   **Render bepul tarifida:** Bot Long Polling bo'lganligi sababli, agar xizmat bepul tarif chekloviga ko'ra uxlab qolsa (sleep state), bot to'xtaydi. Kimdir veb-saytga kirsagina ilova uyg'onib, yig'ilib qolgan telegram xabarlarni o'qib javob qaytaradi. Bunga webhook yechim bo'lishi mumkin edi.

## 8. TUGALLANMAGAN QISMLAR

*   Kodda ochiqdan-ochiq `TODO`, `FIXME` yoki bo'sh qoldirilgan "not implemented" metodlar yo'q.
*   `AGENTS.md` va `FEATURE_IMPLEMENTATION_PLAN.md` hujjatlarida Telegram bot integratsiyasi bo'yicha deyarli hech narsa yozilmagan.
*   Inline tugma (callback) orqali vazifa o'zgartirilganda eski xabar tahrirlanmagan (`editMessageText`), yangisi jo'natilyapti.

## 9. XATO VA XAVFSIZLIK

1.  **CRITICAL:** Bot inline tugmalarida ruxsat tekshiruvi yo'q. `TASK_STATUS_CHANGE` (319-qator) va `TASK_VIEW` handlerlarida tugmani bosgan shaxs vazifaga aloqadormi, yo'qmi tekshirilmagan. Istalgan foydalanuvchi botdagi xabarni boshqa guruhga "forward" qilsa, boshqalar ham bosib vazifani o'zgartirishi mumkin.
2.  **CRITICAL:** Xabar yuborish API so'rovini bloklaydi. `TaskService` dagi 463-qatorda bot orqali xabar yuborish sinxron. Agar Telegram serveri timeout bersa, foydalanuvchining veb saytdagi amali muzlab qoladi.
3.  **MEDIUM (Taxmin):** Token muddati e'tiborga olinmayapti. `UserService` da token uchun `expires_at` o'rnatilsa ham, `TelegramBotService` 90-qatorda faqat token orqali foydalanuvchini izlaydi, vaqti o'tganligini tekshirmaydi.
4.  **MEDIUM:** Rate limit (chiqish so'rovlarini cheklash) mavjud emas. Bot juda ko'p xabarlar yuborib yuborsa, Telegram bloklashi mumkin.
5.  **MEDIUM:** Foydalanuvchi botni o'chirib yuborsa (bloklasa), xabar yuborishda 403 xatosi qaytadi. Bunday holatda ushbu foydalanuvchining `telegramChatId` maydoni `null` qilinib, tozalab tashlanishi kerak, ammo hozir logga yozish bilan cheklanilgan. Tizim botga qayta-qayta uraveradi.
6.  **MEDIUM:** Ma'lumotlar bazasida `telegram_link_token` va `telegram_chat_id` ustunlariga index qo'yilmagan, foydalanuvchilar soni oshganda Full Table Scan tufayli bot sekinlashadi.
7.  **LOW:** Eski tokenni tozalash chala. 94-qatorda `user.setTelegramLinkToken(null)` ishlatilgan, biroq uning vaqti (`telegram_link_token_expires_at`) ham null qilinishi kerak edi.
8.  **LOW:** Takroriy xato tashlash yoki Retry yo'q. 350-qatorda xabar yuborilmaganda, qutqaruv yo'q.
9.  **MEDIUM (Taxmin):** `TaskService` dagi qaramlik masalasi. `@Lazy TelegramBotService telegramBotService` chaqirilgan, lekin token yozilmasa bean hosil bo'lmaydi va xatolikka olib kelishi mumkin (agar if shartida null tushmasa).
10. **LOW:** Global exception handler. Botning ichki xatolari global tutuvchidan (ControllerAdvice kabi) chetda qolib faqat konsolga chiqmoqda, foydalanuvchi esa (masalan xato buyruq bersa) bot javob qaytarmaganidan hayron qolishi mumkin.

## 10. TESTLAR

*   `UserServiceTest` faylida token to'g'ri (SecureRandom) va 15 daqiqa muddatga yaratilayotganini tekshiruvchi testlar mavjud (144-196-qatorlar).
*   **Kamchilik:** `TelegramBotService` logikasini bevosita qoplaydigan birorta ham unit test (bot qanday ishlayotgani, inline keyboardlar qanday javob qaytarayotgani) yozilmagan.

## 11. XULOSA

Telegram botning umumiy ishlash holati **taxminan 40-50% tayyor** deb baholash mumkin.

**Tayyor (ishlaydigan) qismlar:**
*   Foydalanuvchini bot orqali muvaffaqiyatli ulash/uzish.
*   Maqom va faol vazifalarni xabarda, inline tugmalarda ko'rish.
*   Vazifa biriktirilganda xabar yuborilishi.

**Tugatish uchun ustuvor vazifalar:**
1.  **Xavfsizlik:** Bot orqali keladigan call_data (`TASK_STATUS_CHANGE`) harakatlariga foydalanuvchi huquqlarini tekshirishni (auth) darhol kiritish.
2.  **Unumdorlik:** Xabar jo'natuvchi metodni orqa fonga (Async Thread/Message Queue) o'tkazish.
3.  **Ishonchlilik:** Foydalanuvchi botni bloklaganda (TelegramApiException) uning chatId'sini bazadan tozalash. Token muddatini (expires_at) tekshiruvchi logikani `handleStart` ga qo'shish.
4.  **Infratuzilma:** Render uxlab qolishini hisobga olib botni webhook stilyaga (yoki ping bilan uyg'oq tutish usuliga) o'tkazish tavsiya etiladi.
