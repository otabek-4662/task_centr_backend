# Telegram Bot va Oqimlar Bo'yicha Xatolar (BUGS_A)

## High

### 1. Uzun vazifa tafsilotlari sababli Telegram xabarlarining 4096 belgidan oshib ketishi
- **Severity:** High
- **File:Line:** `src/main/java/com/taskcenter/service/TelegramBotService.java:1333`
- **What is wrong:** `buildTaskViewText` metodida vazifaning to'liq tafsiloti (`task.getDescription()`) xabarga qo'shiladi. Telegram API xabarlar uchun 4096 belgilik cheklovga ega. Agar tafsilot juda uzun bo'lsa, xabar yuborishda `TelegramApiException` yuzaga keladi va bot foydalanuvchiga hech qanday javob qaytarmaydi (jim qoladi).
- **How to reproduce:** 4000 belgidan uzun tafsilotga ega vazifa yarating va bot orqali `/my_tasks` tugmasini bosib, o'sha vazifani oching. Bot hech narsa qaytarmaydi.
- **Minimal fix:** `task.getDescription()` ni xabarga qo'shishdan oldin ma'lum uzunlikkacha (masalan, 3000 belgi) kesish (truncate) kerak.

### 2. 100 dan ortiq inline tugmalar sababli botning buzilishi (BUTTONS_TOO_MUCH)
- **Severity:** High
- **File:Line:** `src/main/java/com/taskcenter/service/TelegramBotService.java:835`
- **What is wrong:** `sendTasksList` metodida har bir vazifa uchun 2 ta qator (row) inline tugma qo'shiladi. Telegram inline tugmalar uchun maksimal 100 ta tugma (yoki qator) chekloviga ega. Foydalanuvchida 50 dan ortiq faol vazifa bo'lsa, tugmalar soni 100 dan oshadi va API `Bad Request: BUTTONS_TOO_MUCH` xatoligini beradi. Natijada vazifalar ro'yxati umuman yuborilmaydi.
- **How to reproduce:** Bitta foydalanuvchiga 51 ta vazifa biriktiring va botda `/my_tasks` buyrug'ini bering. Xabar yuborilmaydi.
- **Minimal fix:** `sendTasksList` da vazifalar ro'yxatini cheklash (masalan, eng so'nggi 10 ta vazifa) yoki sahifalash (pagination) qo'shish.

## Medium

### 3. Vazifa yaratish holatidagi (TaskCreationState) ma'lumotlarning yo'qolishi
- **Severity:** Medium
- **File:Line:** `src/main/java/com/taskcenter/service/TelegramBotService.java:104`
- **What is wrong:** `taskCreationStates` xotirada saqlanuvchi `ConcurrentHashMap` hisoblanadi. Bot dasturi (server) qayta ishga tushganda, barcha foydalanuvchilarning chala qolgan vazifa yaratish sessiyalari yo'qolib ketadi. Bundan tashqari, map to'lib ketish ehtimoli mavjud, chunki eskirgan sessiyalar faqat tasodifiy murojaatlardagina tozalanadi.
- **How to reproduce:** `/create_task` buyrug'ini bering, sarlavha kiriting. Serverni qayta ishga tushiring. Keyin g'alvani tanlang - bot "Jarayon muddati o'tgan" deya xatolik beradi.
- **Minimal fix:** `TaskCreationState` ma'lumotlarini bazada (masalan, Redis yoki PostgreSQL) saqlash.

### 4. Boshqa foydalanuvchilarning vazifalarini "Bajarildi" yoki "Ertaga suring" qilish huquqi
- **Severity:** Medium
- **File:Line:** `src/main/java/com/taskcenter/service/TelegramBotService.java:1194`, `1251`
- **What is wrong:** `handleTaskDone` va `handleTaskSnooze` metodlarida faqat `authorizationService.checkCanEdit(task.getWorkspaceId(), user)` orqali tekshiruv o'tkaziladi. Bu degani, g'alvada (workspace) tahrirlash huquqiga ega bo'lgan har qanday foydalanuvchi BOSHQA birovga biriktirilgan vazifani ham bot orqali yakunlashi yoki surishi mumkin.
- **How to reproduce:** O'zingiz a'zo bo'lgan, ammo boshqa odamga biriktirilgan vazifaning Telegram URL/ID sini botga tashlang va inline tugmalarni bosing. Vazifa o'zgaradi.
- **Minimal fix:** Foydalanuvchi ushbu vazifaning `getAssignees()` ro'yxatida bormi yoki yo'qligini ham qo'shimcha ravishda tekshirish kerak (agar g'alva admini bo'lmasa).

### 5. Noma'lum STATS_ va CT_ tugmalarida answerCallbackQuery chaqirilmasligi oqibatida spinnerning qotib qolishi
- **Severity:** Medium
- **File:Line:** `src/main/java/com/taskcenter/service/TelegramBotService.java:1101`
- **What is wrong:** `handleCallbackQuery` dagi asosiy tekshiruvda `STATS_` va `CT_` prefikslari avtomatik `answerCallback` dan mustasno qilingan. Ammo aniq mos kelmaydigan qiymat yuborilsa, kod `else` blokiga tushib "Bu tugma eskirgan..." degan xabar yuboradi, lekin `answerCallbackQuery` ni chaqirmaydi. Natijada tugmadagi aylanuvchi spinner qotib qoladi.
- **How to reproduce:** Eski xabardagi noma'lum `STATS_` yoki `CT_` bilan boshlanuvchi tugmani bosing. Spinner aylanib qolib ketadi.
- **Minimal fix:** `handleCallbackQuery` oxiridagi `else` blokiga `answerCallback(callbackQuery, null);` qatorini qo'shish.

### 6. Telegram xatoliklari (Exception) foydalanuvchiga bildirilmasdan jim yutib yuborilishi
- **Severity:** Medium
- **File:Line:** `src/main/java/com/taskcenter/service/TelegramBotService.java:1166`, `1318`
- **What is wrong:** `executeWithRetry` va `editTaskViewMessage` larda `TelegramApiException` ushlanganda u faqat log qilinadi. Foydalanuvchiga uning so'rovi xato yakunlangani haqida hech qanday xabar ketmaydi, bot shunchaki jim qoladi.
- **How to reproduce:** Bot xabar jo'natishida ataylab xatolik (masalan 400 Bad Request) keltirib chiqaring (masalan juda uzun formatlangan matn). Bot javob bermaydi.
- **Minimal fix:** Kutilmagan xatoliklar ushlanganda (403 dan tashqari), foydalanuvchiga "Xatolik yuz berdi" mazmunida oddiy SMS yuborish mexanizmini qo'shish.

## Low

### 7. Telegram akkauntni ulashda eski bog'lanishlarning shartli tekshiruvisiz ustidan yozib yuborilishi
- **Severity:** Low
- **File:Line:** `src/main/java/com/taskcenter/service/TelegramBotService.java:273`
- **What is wrong:** `/start {token}` orqali akkaunt ulanganda `user.setTelegramChatId(chatId)` bajariladi. Agar boshqa akkaunt allaqachon shu chatga ulangan bo'lsa uni ogohlantiradi, lekin agarda SHU foydalanuvchi allaqachon boshqa chatga (yoki boshqa Telegram raqamga) ulangan bo'lsa, uni ogohlantirmasdan ustidan yozib yuboradi.
- **How to reproduce:** Bitta hisobni avval A telegramdan, keyin B telegramdan ulang. A telegramdagi foydalanuvchi indamasdan botdan uziladi.
- **Minimal fix:** Eskisini uzishdan oldin foydalanuvchini ogohlantirish yoki avvalgi `chatId` ga xabar yuborish.

## Eng birinchi tuzatish kerak bo'lgan 5 ta
1. Uzun vazifa tafsilotlari sababli Telegram xabarlarining 4096 belgidan oshib ketishi (High)
2. 100 dan ortiq inline tugmalar sababli botning buzilishi (BUTTONS_TOO_MUCH) (High)
3. Boshqa foydalanuvchilarning vazifalarini "Bajarildi" yoki "Ertaga suring" qilish huquqi (Medium)
4. Vazifa yaratish holatidagi (TaskCreationState) ma'lumotlarning yo'qolishi (Medium)
5. Noma'lum STATS_ va CT_ tugmalarida answerCallbackQuery chaqirilmasligi oqibatida spinnerning qotib qolishi (Medium)

## Tekshirib bo'lmaganlar
- **Shubhali: O'chirilgan (deleted_at) yoki nofaol foydalanuvchilar botdan foydalana olishi:** `userRepository.findByTelegramChatId(chatId)` so'rovi mantiqan o'chirilgan foydalanuvchilarni qamrab oladimi? Agar ha bo'lsa, o'chirilgan foydalanuvchilar hali ham botdan ma'lumot olishi mumkin. Buni Hibernate `@Where` qoidalaridan tekshirish zarur.
- **Shubhali: Arxivlangan yoki o'chirilgan vazifalarni ko'rish / holatini o'zgartirish:** `TASK_DONE_` va `TASK_SNOOZE_` larda `isArchived` hamda `deletedAt` tekshirilgan. Biroq `TASK_VIEW_` va `TASK_STATUS_CHANGE_` callbacklarida tekshirilmagan. O'chirilgan vazifani holatini o'zgartirish ehtimoli mavjud, uni bazaviy zaproslar darajasida himoyalanganligini tekshirish kerak.
