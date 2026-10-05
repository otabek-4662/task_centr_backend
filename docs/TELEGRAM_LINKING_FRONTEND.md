# 🔗 Telegram Botni Akkauntga Ulash — Frontend Qo'llanmasi

Sayt foydalanuvchisi o'z akkauntini Telegram botga ulaydi. Saytda hozircha Telegram UI yo'q — hammasi noldan quriladi.

> Backend tomoni tayyor: ulanish holati (`telegramLinked`), tayyor havola (`link`), muddat (`expiresAt`) va uzish (`DELETE`) endpointi mavjud.

---

## 🎯 1. Maqsad

Ulangandan keyin:
1. **Bildirishnomalar** (vazifa biriktirilishi, `@mention`, muddat eslatmasi, kunlik digest) Telegramga keladi.
2. **Mini App** (`/app/`) parolsiz ochiladi — foydalanuvchi Telegram ID orqali taniladi.

---

## 🔄 2. Jarayon (backend haqiqatda shunday ishlaydi)

1. Foydalanuvchi saytda **"Telegramni ulash"** ni bosadi → frontend `GET /api/users/me/telegram-link-token` chaqiradi.
2. Backend 8 belgili kod (`A–Z`, `0–9`) yaratadi, foydalanuvchiga saqlaydi, **15 daqiqa** muddat qo'yadi va tayyor `link` qaytaradi.
3. Frontend `link` ni yangi oynada ochadi → Telegram bot ochiladi (`https://t.me/<bot>?start=<kod>`).
4. Foydalanuvchi botda **START** bosadi (yoki qo'lda `/start <kod>` yozadi).
5. Bot kodni tekshiradi (mavjudmi, muddati o'tmaganmi, bu Telegram boshqa akkauntga ulanmaganmi), `telegramChatId` ni akkauntga yozadi, kodni o'chiradi va xabar yuboradi: *"🎉 Davraga xush kelibsiz … Akkauntingiz muvaffaqiyatli ulandi"*.
6. Saytda `GET /api/me` → `telegramLinked: true`.

Bot xatolari (foydalanuvchiga **botda** ko'rinadi, saytga kelmaydi):
- kod noto'g'ri yoki eskirgan (15 daqiqa) → yangi kod olish kerak;
- 15 daqiqada 5 tadan ko'p noto'g'ri urinish → 15 daqiqa kutish;
- bu Telegram boshqa akkauntga ulangan → bitta Telegram — bitta akkaunt.

---

## 📡 3. Endpointlar

Hammasida: `Authorization: Bearer <jwt>`. Javoblar `ApiResponse` ichida: `{ success, message, data, timestamp }`.

### 3.1 Ulash havolasini olish
- `GET /api/users/me/telegram-link-token`
- Body: yo'q

**200 OK:**
```json
{
  "success": true,
  "message": "ok",
  "data": {
    "token": "E4X92NPL",
    "link": "https://t.me/task_center_bot?start=E4X92NPL",
    "expiresAt": "2026-10-05T15:30:00.123456"
  },
  "timestamp": "2026-10-05T15:15:00.123456"
}
```
- `token` — eski maydon, saqlab qolingan.
- `link` — bot username backend sozlamasida (`TELEGRAM_BOT_USERNAME`) berilmagan bo'lsa **umuman qaytmaydi** (maydon yo'q). Bunda frontend havolani o'zi yig'maydi — backend `.env` ni to'ldirishini so'rang.
- `expiresAt` — server vaqti (`LocalDateTime`, timezone belgisiz). Faqat ko'rsatish uchun ishlating; asosiy mantiq: **har bosishda yangi kod olinadi**, eski kod almashtiriladi.

**Xatolar:** token yo'q / yaroqsiz / muddati o'tgan JWT → `403` (backendda hozir shunday: 401 emas, 403).

### 3.2 Ulanganlik holati
- `GET /api/me` (alias: `GET /api/auth/me`)

**200 OK:**
```json
{
  "success": true,
  "message": "ok",
  "data": {
    "id": "c1f7a08b-b384-4fe1-...",
    "name": "xusanboy",
    "fullName": "Xusanboy Developer",
    "email": "xusanboy@example.com",
    "role": "USER",
    "taskCount": 12,
    "statusNickname": "O'zimizdan",
    "telegramLinked": true
  },
  "timestamp": "2026-10-05T15:15:00.123456"
}
```
`telegramLinked` ham `PATCH /api/users/me` javobida, ham login/register javobidagi `data.user` ichida keladi. Xom `telegramChatId` hech qachon qaytmaydi.

### 3.3 Telegramni uzish
- `DELETE /api/users/me/telegram`
- Body: yo'q. Faqat **joriy foydalanuvchi** uchun ishlaydi (boshqa userni uzib bo'lmaydi). Chat ID va ulash kodi tozalanadi.

**200 OK:**
```json
{
  "success": true,
  "message": "Telegram akkaunt uzildi",
  "data": null,
  "timestamp": "2026-10-05T15:20:00.123456"
}
```
**Xatolar:** JWT yo'q/yaroqsiz → `403`. Ulanmagan foydalanuvchida ham `200` qaytadi (idempotent).

---

## 🔍 4. Ulanganini qanday bilamiz?

`GET /api/me` → **`data.telegramLinked`** (`true` / `false`).

---

## 🖥️ 5. UI

Profil / Sozlamalar sahifasiga blok qo'shing:

| Holat | Ko'rinish |
|---|---|
| `telegramLinked: false` | "Telegram: ulanmagan ⚪" + **"Telegramni ulash"** tugmasi |
| kod olingan, kutilmoqda | "Botda START bosing. Kod 15 daqiqa amal qiladi." + **"Qayta urinish"** (yangi kod oladi) |
| `telegramLinked: true` | "Telegram: ulangan 🟢" + **"Uzish"** tugmasi (tasdiqlash oynasi bilan) |

Muhim nuqtalar:
- Tugma bosilganda `link` ni `window.open(link, '_blank')` bilan oching.
- Foydalanuvchi Telegramdan saytga qaytganda (`focus` / `visibilitychange`) `GET /api/me` ni qayta chaqiring.
- Kutish holatida `expiresAt` o'tib ketsa, "Kod eskirdi" deb ko'rsating va "Qayta urinish" ni taklif qiling (kod yangilanadi).
- Ixtiyoriy: kutish paytida har 3–5 soniyada `GET /api/me` (polling), `telegramLinked: true` bo'lsa to'xtating.

```javascript
const API = 'http://localhost:8080';
const auth = () => ({ Authorization: `Bearer ${localStorage.getItem('token')}` });

// "Telegramni ulash" tugmasi
async function connectTelegram() {
  const res = await fetch(`${API}/api/users/me/telegram-link-token`, { headers: auth() });
  if (!res.ok) throw new Error('Kod olib bo\'lmadi: ' + res.status);
  const { data } = await res.json();

  if (!data.link) {
    alert('Telegram bot sozlanmagan. Administratorga murojaat qiling.');
    return;
  }
  window.open(data.link, '_blank');
  showWaiting(new Date(data.expiresAt)); // "Botda START bosing" + muddat
}

// Holatni tekshirish
async function refreshTelegramStatus() {
  const res = await fetch(`${API}/api/me`, { headers: auth() });
  if (!res.ok) return;
  const { data } = await res.json();
  renderTelegram(data.telegramLinked); // true -> "ulangan 🟢" + "Uzish"
}
window.addEventListener('focus', refreshTelegramStatus);

// "Uzish"
async function disconnectTelegram() {
  if (!confirm('Telegramni uzmoqchimisiz? Bildirishnomalar to\'xtaydi.')) return;
  const res = await fetch(`${API}/api/users/me/telegram`, { method: 'DELETE', headers: auth() });
  if (res.ok) refreshTelegramStatus();
}
```

---

## 📌 6. Eslatma

- `/app/` dagi Mini App backend tomonidan yaratilgan — frontend dasturchi unga **tegmaydi**.
- Mini App ichida avtomatik kirish imkoniyati mavjud. Agar foydalanuvchi akkaunti hali Telegram bilan ulanmagan bo'lsa, Mini App ichida to'g'ridan-to'g'ri tizimga kirish va akkauntni ulash uchun maxsus ekran (Login View) ko'rsatiladi. Bu orqali foydalanuvchi ulanishni to'g'ridan-to'g'ri Telegramning o'zida ham amalga oshirishi mumkin.
- Web sayt orqali ulash UI ham baribir o'z kuchida qoladi (shaxsiy kabinet orqali ulanish imkoniyati uchun).
- Uzilgandan keyin Mini App yana Login ekranini ko'rsatadi.
