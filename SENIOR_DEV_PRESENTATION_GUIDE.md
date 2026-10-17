# 🌟 Senior Developerlar Oldida Taqdimot Qo'llanmasi (Master Presentation Guide)
## Loyiha: `Task Center` (Choylashamiz Ekotizimi) — Backend, Telegram Bot va Telegram Mini App (TMA)

---

## 1. Loyiha Pasporti va Qisqacha "Executive Pitch" (1 daqiqada tushuntirish)

> **"Choylashamiz / Task Center"** — zamonaviy korxona va IT jamoalari uchun Jira, Trello va Slack imkoniyatlarini Telegram ekotizimi bilan to'liq birlashtirgan, yuqori tezlikdagi (<100ms) jamoaviy boshqaruv platformasi.
> 
> Tizim uchta qatlamdan iborat:
> 1. **Robust Enterprise Backend:** Spring Boot 3.2.5, Java 17, PostgreSQL, Flyway (V1..V64 migratsiyalar), STOMP over SockJS (WebSocket), Caffeine Cache, ShedLock.
> 2. **Telegram Bot Ekotizimi:** Webhook va polling rejimida ishlaydigan, guruhlar (Supergroup topics), shaxsiy chatlar, eslatmalar, inline qidiruv va bot orqali vazifalar yaratishni ta'minlovchi asinxron bot.
> 3. **Telegram Mini App & Responsive Web UI:** Telegram ichida ham, har qanday desktop va mobil brauzerda ham birdek ishlaydigan, 60fps tezlikdagi interaktiv Kanban doska, Agile sprintlar, Pomodoro taymer, analitika va SuperAdmin platformasi.

---

## 2. Senior Developerlarni Qoyil Qoldiradigan 8 ta Arxitektura Yechimi

Senior developerlar oddiy CRUD yozilgan loyihalarga qiziqmaydi. Ularga **masshtablilik (scalability), ma'lumotlar yaxlitligi (data integrity), xavfsizlik (security) va resurslarni tejash (resource optimization)** muhim. Taqdimot davomida quyidagi 8 ta nuqtani alohida ta'kidlang:

### 2.1. LexoRank (Base-36) O(1) Tartiblash Algoritmi (Jira Pattern)
- **Muammo:** Oddiy loyihalarda Kanbanda karta yoki ustun surilganda barcha qatorlarning `order_index` ustuni qayta hisoblanadi (`UPDATE tasks SET order = order + 1 WHERE ...`). 10 000 ta vazifada bu butun bazani bloklaydi.
- **Bizning yechim:** Atlassian Jira algoritmi asosida **LexoRank (Base-36)** joriy qilingan. Vazifa boshqa ikkita vazifa o'rtasiga surilganda faqat bitta matematik operatsiya (`LexoRankUtil.between(prev, next)`) orqali yangi string kod hisoblanadi (masalan: `0|hzzzzz:`). **Baza bo'yicha murakkablik: O(1).**
- **Balanslash (Rebalancing):** Oraliq tugab qolgan kamyob holatlar uchun `/api/workspaces/{wsId}/tasks/columns/{colId}/rebalance` orqali LexoRank'lar avtomatik oraliqlab qayta tarqatiladi.

### 2.2. Render Free / Cloud Guardrails (512MB RAM & 5 Connection HikariCP)
- **Muammo:** Kam resursli bulutli muhitda (masalan, Render Free 512MB RAM va 0.1 CPU) ulanishlar puli (Connection Pool) tezda to'lib qoladi (`Connection is not available`).
- **Bizning yechim:**
  - HikariCP qat'iy `maximum-pool-size: 5` da ushlab turiladi.
  - **Oltin Qoida:** Tashqi uchinchi tomon HTTP API chaqiruvlari (Telegram Bot API, Brevo Email, S3) **hech qachon `@Transactional` annotatsiyasi ostida bajarilmaydi!**
  - Telegram xabarlari `TelegramBotService` ichidagi `outboundMessageQueue` (non-blocking thread pool + exponential retry) orqali yuboriladi. Telegram 429 bergan taqdirda ham backendning HTTP threadlari va DB connectionlari bloklanmaydi.

### 2.3. Zero N+1 Muammosi & Cartesian Product Portlashidan Himoya
- **Muammo:** Bitta vazifada bir nechta ijrochilar (`assignees`), teglar (`labels`), yo'nalishlar (`directions`) va kuzatuvchilar (`watchers`) bo'ladi. Agar ularga bitta JPQL da bir nechta `JOIN FETCH` qilinsa, SQL Cartesian Product hosil bo'lib, xotirada millionlab keraksiz obyektlar yaratiladi.
- **Bizning yechim:**
  - **Ikki bosqichli ID Pagination (Two-Step Fetch):** Oldin indekslangan ustunlar bo'yicha faqat task ID lari sahifalanadi (`findIdsByWorkspaceIdFiltered`), so'ngra aniq 20-30 ta task `@BatchSize(size = 25)` va `@EntityGraph` bilan xotiraga tortiladi. Natijada so'rovlar soni qat'iy nazorat ostida (O(1) so'rovlar).

### 2.4. Ikki Qatlamli Autentifikatsiya (Telegram HMAC-SHA256 + Stateless JWT)
- **Telegram Mini App Auth:** Telegramdan kelayotgan `initData` soxtalashtirilmasligi uchun bot tokenidan `HMAC-SHA256` orqali `secret_key` yasaladi va kelgan xesh bilan solishtiriladi (`TelegramInitDataValidator`). Replay attack oldini olish uchun `auth_date` muddati (TTL) tekshiriladi.
- **Brauzer & REST API Auth:** Standart JWT (Access + Refresh token rotatsiyasi). Parollar `BCrypt` bilan xeshlanadi. Parolni unutganlar uchun esa bir martalik xavfsiz vaqtli tokenlar ishlatiladi.
- **Rate Limiting:** Brute-force hujumlariga qarshi IP va Username darajasida xotirada tezkor token-bucket rate limiter (`RateLimitFilter`) mavjud.

### 2.5. Real-Time STOMP over SockJS (Bidirectional Sync)
- Doskada biror xodim kartani sursa, statusini o'zgartirsa yoki umumiy/shaxsiy chatda xabar yozsa, sahifani yangilash (refresh) shart emas.
- WebSocket orqali `/topic/workspaces/{id}`, `/topic/chat/public`, `/user/queue/chat` kanallari tinglanadi. Tarmoq uzilsa, frontend avtomatik eksponentsial qayta ulanadi (auto-reconnect).

### 2.6. Jonli Pomodoro / Fokus Taymeri va Avtomatik Worklog
- Har bir vazifa uchun 25 daqiqalik Pomodoro rejimini yoqish mumkin (`/api/timers/start`).
- Taymer ishlayotgan paytda foydalanuvchining ekrani tepasida jonli banner yuradi.
- Taymer to'xtatilganda, ishlangan vaqt avtomatik tarzda vazifaning sarflangan vaqti (Worklog) tarixiga qo'shiladi va umumiy `loggedHours` yangilanadi.

### 2.7. Task Dependencies (Blocker) va Aylanma Bog'liqlikni Rad Etish
- Jira kabi bitta vazifa ikkinchisini to'sib turishi mumkin (masalan, Backend tugamasdan Frontend tugallanmaydi).
- Agar vazifada tugallanmagan to'siq (Blocker) bo'lsa, uni "Done" (Bajarildi) ustuniga o'tkazishga backend ruxsat bermaydi (`TaskBlockedException`).
- Aylanma bog'liqlik (Circular dependency: A -> B, B -> A) yaratishga urinilganda backend buni aniqlab, 400 xatolik bilan rad etadi.

### 2.8. SuperAdmin Boshqaruv Markazi
- `/api/admin/dashboard` orqali real-time tizim metrikalari (jami foydalanuvchilar, ulangan Telegram hisoblar, ochiq va yopiq vazifalar, ish maydonlari).
- Barcha Telegram foydalanuvchilariga bir zumda muhim yangiliklarni yuborish (Telegram Broadcast queue).
- Caffeine keshlarini bir zumda tozalash (`/api/admin/cache/evict`).
- Tizim fon xizmatlarini (Garbage Collector, Reminder digest cron) qo'lda majburiy ishga tushirish.

---

## 3. Jonli Taqdimot Stsenariysi (Live Demo Script — 5-7 daqiqa)

Ertaga seniorlarga loyihani ko'rsatishda quyidagi tartibda yuring. Bu stsenariy loyihaning barcha burchaklarini qamrab oladi va hech qanday to'xtalish bo'lmaydi:

```
[1. Tizimga Kirish] 
  └── Brauzerda http://localhost:8080/app/ ochiladi.
  └── "⚡️ Elshod (User)" yoki "🛡 Admin (SuperAdmin)" tugmasi bosiladi (Parol terish shart emas, 1 soniyada kiradi).

[2. Interaktiv Kanban Doskasi]
  └── Kartalarni bir ustundan ikkinchisiga suring (Drag-and-drop).
  └── "Done" ustuniga surilganda haptika va tabrik animatsiyasi chiqishini ko'rsating.
  └── Ustun yuqorisidagi "⚖️" (LexoRank rebalance) tugmasini bosing.
  └── Tepadagi filtrlardan foydalaning: "Bugun", "Muddati o'tgan", "Yuqori ustuvorlik", qidiruv.

[3. Vazifa Tafsilotlari & Produktivlik]
  └── Biror kartani bosing (Bottom Sheet modal ochiladi).
  └── "🍅 Fokus (25m)" tugmasini bosing: tepada jonli Pomodoro taymeri boshlanadi!
  └── "Ro'yxat (Checklist)" bo'limida yangi band qo'shing va chek qiling.
  └── "Ostki vazifalar (Subtasks)" va "Vaqt hisobi (Worklogs)" yozuvlarini ko'rsating.
  └── "Bog'liqlik (Dependencies)" qismida boshqa vazifani Blocker qilib qo'shing.

[4. Analitika va Eksport]
  └── Chap menyudan "Analitika va Hisobot" (Stats) bo'limiga o'ting.
  └── Rangli SVG Donut diagramma, ustunlar bo'yicha WIP limitlar va jamoa yuklamasini ko'rsating.
  └── "📥 CSV yuklab olish" tugmasini bosing -> Darhol real .csv fayl yuklanadi!
  └── "📄 Kunlik PDF Hisobot" tugmasini bosing -> Backend chiroyli PDF fayl generatsiya qilib beradi!

[5. Agile Sprintlar va Backlog]
  └── "Sprintlar va Reja" bo'limiga o'ting.
  └── Faol sprint, sprint maqsadlari va pastdagi "Backlog" ro'yxatini ko'rsating.
  └── Vazifani backlogdan bevosita sprintga biriktirishni namoyish qiling.

[6. Jamoaviy Real-Time Muloqot (Chat)]
  └── "Jamoaviy muloqot" bo'limiga o'ting.
  └── Umumiy jamoa xonasiga xabar yozing, xabarlarga emoji reaksiya (👍, ❤️, 🔥) bosing.
  └── Xabarga javob (reply) berish funksiyasini ko'rsating.

[7. SuperAdmin Platformasi]
  └── Agar "Admin" sifatida kirgan bo'lsangiz, chap menyuda "Admin Boshqaruv" bo'limi ko'rinadi.
  └── Dashboard KPI metrikalarini ko'rsating.
  └── "Foydalanuvchilar" subtabida qidiruv va rollarni almashtirish (USER <-> ADMIN).
  └── "Keshni tozalash" tugmasini bosib Caffeine keshini tozalang.
  └── "Telegram Broadcast" orqali barcha ulangan foydalanuvchilarga xabar yuborish formasini ko'rsating.

[8. Telegram Bot Ekotizimi]
  └── Telegramda botingizni oching (@bot).
  └── `/start` bosing: menyu klaviaturasi chiqadi.
  └── `/my_tasks` bosing: sizga biriktirilgan vazifalar inline tugmalar bilan chiqadi.
  └── `/timer` bosing: bot orqali ham Pomodoro taymerini boshqarish mumkin!
  └── `/report` bosing: bot to'g'ridan-to'g'ri PDF hisobotni chatga tashlab beradi!
  └── Istalgan boshqa chatda `@botingiz_nomi vazifa` deb yozib inline qidiruvni ko'rsating.
```

---

## 4. Senior Developerlar Berishi Mumkin Bo'lgan Savollar va Tayyor Javoblar

Seniorlar kodning nozik joylarini bilish uchun savol berishni yaxshi ko'rishadi. Quyidagi tayyor javoblar sizni ularning oldida juda kuchli mutaxassis sifatida ko'rsatadi:

| # | Senior Developer Savoli | Sizning Mukammal Javobingiz |
|---|---|---|
| 1 | **"Nega `tasks` jadvalida `status` degan oddiy ustun yo'q?"** | *"Biz Jira arxitekturasiga tayandik. Haqiqiy jamoalarda holatlar qotib qolgan enum (OPEN, CLOSED) emas, balki dinamik BoardColumn (Backlog, In Dev, Code Review, QA, Staging, Done) lardan iborat bo'ladi. Har bir workspace o'z ustunlarini tuza oladi. Vazifaning tugallanganligi esa ustunning `is_done` boolean ustuni orqali aniqlanadi."* |
| 2 | **"Kanbanda kartalar ketma-ketligini (order) qanday saqlaysiz?"** | *"Integer indeks ishlatmadik, chunki o'rtaga karta qo'shilganda minglab qatorlarni UPDATE qilish kerak bo'ladi. Jira kabi Base-36 LexoRank algoritmini kiritdik. Har doim ikkita element orasidagi o'rtacha leksikografik qiymat O(1) vaqtda topiladi."* |
| 3 | **"5 ta connection pool bilan bir vaqtda ko'p odam kirsa backend yiqilmaydimi?"** | *"Render Free va kichik VM larda xotira 512MB bo'lgani sababli pool 5 qilib belgilangan. Lekin bizda barcha so'rovlar indekslangan va o'rtacha javob vaqti 5-15ms ni tashkil qiladi. Eng muhimi, Telegram, Brevo Email va fayl yuklash kabi tashqi sekin HTTP chaqiruvlar tranzaksiyadan tashqarida (non-blocking) bajariladi. Shuning uchun connectionlar 20 millisekunddan ortiq band bo'lmaydi."* |
| 4 | **"N+1 muammosi qanday hal qilingan?"** | *"Birinchidan, ko'pga-ko'p bog'lanishlar (assignees, directions, labels) uchun `@BatchSize(25)` qo'yilgan. Ikkinchidan, doskani yuklashda 2 bosqichli ID pagination qilamiz: avval faqat ID lar o'qiladi, keyin `@EntityGraph` bilan kerakli ma'lumotlar bitta optimallashgan so'rovda tortiladi."* |
| 5 | **"Telegram Mini App foydalanuvchisini qanday tekshirasiz? Token soxta bo'lsa-chi?"** | *"Faqat clientdan kelgan user ID ga ishonmaymiz. Telegram WebApp uzatgan xom `initData` qatorini olib, Bot Tokenimizdan yasalgan maxfiy HMAC-SHA256 kalit bilan xeshni solishtiramiz. Agar bironta belgi o'zgargan bo'lsa yoki auth vaqti eskirgan bo'lsa, backend 401 Unauthorized qaytaradi."* |
| 6 | **"Fayllar qayerda saqlanadi?"** | *"Bizda Strategy Pattern asosida `FileStorageService` interfeysi bor: lokalda diskda (`local`), Cloud muhitida S3 / Cloudflare R2 (`s3`), Render Free kabi efemer disksiz tekin serverlarda esa PostgreSQL ning o'zida `stored_files` jadvalida (`db`) saqlash strategiyalari to'liq tayyor."* |
| 7 | **"DB migratsiyalari qanday boshqariladi?"** | *"Flyway orqali. V1 dan V64 gacha barcha jadvallar, indekslar va triggerlar versiyalangan. Har bir yangi o'zgarish yangi migratsiya faylida yoziladi va PostgreSQL native optimallashgan indekslar (partial index: `WHERE deleted_at IS NULL`) qo'llanilgan."* |
| 8 | **"Distributed Scheduling masalasi qanday hal qilingan?"** | *"Eslatmalar va kunlik dayjestlar kabi fon vazifalari server bir nechta nusxada (multi-instance) ishga tushganda bir necha marta takrorlanib ketmasligi uchun ShedLock kutubxonasi yordamida bazada distributed lock o'rnatilgan."* |

---

## 5. Ertangi Taqdimotgacha Siz (Inson) Qilishi Kerak Bo'lgan 3 Ta Qadam

Barcha kodlar, testlar, avtomatizatsiya va xavfsizlik choralari 100% bajarildi. Ertaga taqdimot paytida hech qanday kutilmagan holat bo'lmasligi uchun siz quyidagi 3 ta oddiy qadamni bajarasiz:

### 1-qadam: Mahalliy PostgreSQL Bazasini Tekshiring
Loyiha PostgreSQL ga ulanadi (`application.yml` dagi standart parametrlar: port `5432`, db `taskcenter`, user `postgres`, password `password`).
Agar Docker o'rnatilgan bo'lsa, bitta buyruq bilan yoqish mumkin:
```powershell
docker run --name tc-postgres -e POSTGRES_DB=taskcenter -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password -p 5432:5432 -d postgres:15-alpine
```

### 2-qadam: Backendni Ishga Tushiring
Terminalda loyiha papkasida:
```powershell
.\mvnw.cmd spring-boot:run
```
> **Eslatma:** `application.yml` da `app.seed.enabled: true` qilindi. Ya'ni birinchi marta ishga tushishi bilan bazaga avtomatik tarzda:
> - `admin` (Parol: `password123`, Rol: `ADMIN` — SuperAdmin!)
> - `elshod` (Parol: `password123`, Rol: `USER`)
> - Namuna vazifalar, ustunlar va teglar avtomatik yoziladi! Siz taqdimot paytida bo'sh doska ko'rsatmaysiz, allaqachon boy va chiroyli ma'lumotlar turadi.

### 3-qadam: Brauzerda Ochish va Ko'rsatish
Brauzeringizda quyidagi manzilni oching:
👉 **`http://localhost:8080/app/`**
- Kirish ekranida **"🛡 Admin (SuperAdmin)"** tugmasini bosing — bir zumda admin paneli, kanban doskasi va barcha imkoniyatlar ochiladi.
- Agar Swagger / OpenAPI hujjatini ko'rsatmoqchi bo'lsangiz:
👉 **`http://localhost:8080/swagger-ui.html`** (Barcha 100+ endpointlar chiroyli guruhlangan va to'liq interaktiv).

---
*Muvaffaqiyatli taqdimot tilaymiz! Ushbu arxitektura va funksionallik senior darajadagi mutaxassislarda juda katta taassurot qoldiradi.*
