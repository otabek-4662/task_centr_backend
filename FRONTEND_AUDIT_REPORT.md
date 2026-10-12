# 📋 Task Manager Frontend — To'liq Tahlil va Audit Hisoboti

**Sana:** 2026-10-12  
**Repozitoriy:** [xusanyusupov05/task-manager-frontend](https://github.com/xusanyusupov05/task-manager-frontend)  
**Backend:** [otabek-4662/task_centr_backend](https://github.com/otabek-4662/task_centr_backend) (Spring Boot 3.2.5 + Java 17 + PostgreSQL)

---

## 📌 Ijroiy Xulosa (Executive Summary)

Frontend loyihasi zamonaviy stekda yaratilgan: **Vite 8 + React 19 + TypeScript + Redux Toolkit (RTK Query) + Ant Design 6 + @dnd-kit/react + TailwindCSS**. Arxitektura Feature-Sliced Design (FSD) printsiplariga asoslangan (`src/app`, `src/entities`, `src/pages`, `src/shared`, `src/widgets`). Dizayn va UI komponentlari (o'zbekona "choylashamiz", "ayvon", "g'alvalar" mavzusi) chiroyli ishlangan.

Biroq, **Frontend va Backend integratsiyasida o'ta jiddiy kamchiliklar, yarim qolib ketgan funksiyalar va kritik xatoliklar (Critical Bugs)** mavjud. Eng asosiylari:
1. **Topshiriqlarni (Tasks) surish (Drag-and-Drop) Backend bilan bog'lanmagan** — faqat local React state da o'zgaradi, sahifa yangilansa barcha harakat yo'qoladi.
2. **Topshiriqlar (Tasks) uchun hech qanday API qatlami mavjud emas** (`entities/tasks` mavjud emas, vazifa yaratish, tahrirlash, o'chirish umuman ishlamaydi).
3. **Kanban Board yuklanishida `useGetWorkspaceColumnsQuery` ishlatilgan**, ammo backend dagi `BoardController` (`/workspaces/{workspaceId}/board`) chaqirilmagan. Natijada ustunlar ichidagi topshiriqlar bo'sh bo'lib qoladi.
4. **Base URL va `/api` prefiksi yo'qolgan** — agar `VITE_APP_BASE_URL` ga faqat `http://localhost:8080` berilsa, barcha so'rovlar `404 Not Found` qaytaradi.
5. **Telegram Mini App (TMA) integratsiyasi frontendda mavjud emas** — Mini App orqali kirilganda avtomatik autentifikatsiya qilinmaydi (`initData` yuborilmaydi).

---

## 🔴 1. KRITIK XATOLIKLAR (CRITICAL ISSUES)

### 1.1. Drag-and-Drop (DND) orqali vazifalar harakati Backend ga saqlanmaydi
* **Fayl:** `src/widgets/kanban-board/kanban-board.tsx` (168-189-qatorlar)
* **Muammo:** Ustunlar o'rnini almashtirganda `reorderColumns` mutatsiyasi chaqirilgan, biroq vazifalarni bir ustundan ikkinchisiga ko'chirganda yoki ustun ichida tartiblaganda faqatgina `setColumns((prev) => ...)` (React local state) chaqirilgan.
* **Oqibat:** Foydalanuvchi vazifani boshqa ustunga o'tkazsa yoki o'rnini almashtirsa, sahifa yangilanishi (`F5`) bilanoq barcha o'zgarishlar bekor bo'ladi va eskicha holatiga qaytadi.
* **Backend yechimi:** Backend da `PATCH /api/workspaces/{workspaceId}/tasks/{id}/reorder` mavjud bo'lib, u `prevRank`, `nextRank` va yangi `columnId` ni (LexoRank bo'yicha) qabul qiladi. Frontend ga ushbu API chaqiruvi kiritilishi shart.

### 1.2. Vazifalar (Tasks) CRUD operatsiyalari va API umuman yo'q
* **Fayl:** `src/entities/` va `src/widgets/kanban-board/kanban-column.tsx`
* **Muammo:** 
  - `src/entities/` papkasida `tasks` nomli entity umuman mavjud emas!
  - `kanban-column.tsx` faylida vazifa qo'shish formasi (`openCardAdd`) mavjud, biroq uning tugmasida hech qanday `submit` yoki API mutatsiyasi yo'q (`<Button type="primary">Buni ham dushanbadan qilamiz!</Button>` ga bosilganda hech narsa sodir bo'lmaydi).
  - Kasblar ro'yxati (`Kimga sovg'a qilamz?`) statik yozilgan (`frontend`, `backend`, `design`...), backend dagi foydalanuvchilar (Workspace members) bilan bog'lanmagan.
  - Vazifani tahrirlash (Edit) yoki o'chirish (Delete) imkoniyati yo'q.

### 1.3. Doskani yuklashda to'g'ri bo'lmagan Endpoint ishlatilgan
* **Fayl:** `src/widgets/kanban-board/kanban-board.tsx` (84-87-qatorlar)
* **Muammo:** Kanban doskasi `useGetWorkspaceColumnsQuery({ workspaceId })` orqali `/workspaces/{workspaceId}/columns` endpointiga murojaat qiladi.
* **Oqibat:** `apiMap.ts` da `WORKSPACE_BOARD: (id) => /workspaces/${id}/board` aniqlangan bo'lsa-da, hech qayerda chaqirilmagan. Doska to'liq kartochkalari bilan ochilishi uchun `/api/workspaces/{workspaceId}/board` (`BoardController`) ishlatilishi lozim.

### 1.4. TaskCard komponentida ma'lumotlar statik (Mock) qotirilgan
* **Fayl:** `src/widgets/kanban-board/task-card.tsx`
* **Muammo:**
  ```tsx
  export function TaskCard({
    id,
    labels = ["backend", "frontend"],
    members = ["Yusupov Xusan", "Sotiboldiyev Otabek", "Abdullayev Ixtiyor"],
    title = "Rello refactor",
  }: TaskCardProps)
  ```
  Kartochka pastidagi `WFM-204` kodi, har bir kartochkadagi 3 ta user avatari va etiketkalar (labels) hardcoded yozilgan. Backend `TaskDto` da esa: `priority` (LOW, MEDIUM, HIGH, URGENT), `issueType` (STORY, TASK, BUG, EPIC), `assignee`, `reporter`, `storyPoints` mavjud. Frontend ularni o'qimaydi.

---

## 🟠 2. YUQORI DARAJADAGI XATOLIKLAR (HIGH SEVERITY)

### 2.1. URL marshrutlarida (Routing) Apostrof (`'`) belgisi muammosi
* **Fayl:** `src/shared/consts/routes-path.ts`
* **Kod:**
  ```ts
  export const ROUTE_PATH = {
    HOME: "/",
    KANBAN_MAIN: "/g'alvalar_makoni",
    KANBAN_BOARD: "/bosh_o'g'riqlar",
    PROFILE: "/hujram",
    AUTH: "/auth",
    LOGIN: "/login",
  };
  ```
* **Muammo:** URL larda apostrof (`'`) belgisi mavjudligi brauzerda URL encoding (`%27`), SSR, link ulashish yoki router parametrlarida chalkashliklarni keltirib chiqaradi.
* **Tavsiya:** URL slug lar faqat ASCII harflar va defis bilan bo'lishi kerak: `/galvalar`, `/bosh-ogriqlar`, `/hujram`.

### 2.2. Base URL va API yo'li mos kelmasligi (`404 Not Found` xavfi)
* **Fayl:** `src/shared/api/baseApi.ts` va `src/shared/api/apiMap.ts`
* **Muammo:**
  - `baseApi.ts` da `baseURL = import.meta.env.VITE_APP_BASE_URL`. Hech qanday default qiymat berilmagan.
  - `apiMap.ts` dagi barcha yo'llar `/api` siz yozilgan: `/auth/login`, `/workspaces`, `/workspaces/${id}/columns`.
  - Backend dagi barcha kontrollerlar `/api/...` ostida joylashgan (`/api/auth/login`, `/api/workspaces`...).
  - Agar `.env` da `VITE_APP_BASE_URL=http://localhost:8080` ko'rsatilsa, frontend `http://localhost:8080/auth/login` ga murojaat qilib, `404 Not Found` oladi.
* **Tavsiya:** `apiMap.ts` da `/api` prefiksini qo'shish yoki `baseApi.ts` da `baseURL = import.meta.env.VITE_APP_BASE_URL || "http://localhost:8080/api"` qilib sozlash.

### 2.3. Avtorizatsiyasiz foydalanuvchini yo'naltirish xatosi
* **Fayl:** `src/pages/layout.tsx` (14-16-qatorlar)
* **Kod:**
  ```tsx
  if (!token) {
    return <Navigate to={ROUTE_PATH.AUTH} replace state={{ from: location }} />;
  }
  ```
* **Muammo:** Foydalanuvchi tizimga kirmagan bo'lsa, uni `ROUTE_PATH.AUTH` (ya'ni Ro'yxatdan o'tish / Register) sahifasiga yo'naltiradi. Odatda foydalanuvchi `ROUTE_PATH.LOGIN` (Kirish) sahifasiga yo'naltirilishi kerak.

### 2.4. Tokenlarni saqlash va tozalashdagi nomutanosiblik (Logout Bug)
* **Fayllar:** `src/app/loginSlice.ts`, `src/widgets/shared/auth/login-form.tsx`, `src/widgets/profile/setting.tsx`
* **Muammo:**
  - Kirganda: `localStorage.setItem("accessToken", token)` va `localStorage.setItem("token", token)` ikkalasi saqlanadi.
  - Redux `logout` harakatida esa faqat `localStorage.removeItem("token")` o'chiriladi.
  - `accessToken` o'chirilmay qolsa, `MainLayout` da `localStorage.getItem("accessToken")` mavjud bo'lgani uchun foydalanuvchi yana tizimga kirgan deb hisoblanadi.

### 2.5. 401 Unauthorized va Token muddati tugaganda qayta yo'naltirish yo'qligi
* **Fayl:** `src/shared/api/baseApi.ts`
* **Muammo:** JWT token eskirganida yoki yaroqsiz bo'lganida backend `401 Unauthorized` qaytaradi. `baseApi.ts` faqat `console.error` qiladi. Sahifa esa qotib qoladi yoki xato beradi; foydalanuvchi avtomatik ravishda `/login` ga yo'naltirilmaydi va eski token tozalanmaydi.

---

## 🟡 3. O'RTA VA PAST DARAJADAGI KAMCHILIKLAR (MEDIUM / LOW)

### 3.1. Telegram Mini App (TMA) imkoniyatlaridan foydalanilmagan
* Backend Mini App orqali kelgan `initData` ni avtomatik tekshiradi (`X-Telegram-Init-Data` yoki `Authorization: tma ...`).
* Frontend esa Telegram Mini App muhitini (`window.Telegram?.WebApp`) umuman tekshirmaydi. Telegram ichida ochilganda ham oddiy login/parol so'rab turaveradi.
* **Yechim:** `window.Telegram?.WebApp?.initData` mavjud bo'lsa, `baseApi` orqali `X-Telegram-Init-Data` sarlavhasini avtomatik yuborish yoki backend ning `/api/v1/auth/telegram/webapp` endpointi orqali avtologin qilish.

### 3.2. Bosh sahifa (Home / "Ayvon") bo'sh holatda
* **Fayl:** `src/pages/main/index.tsx`
* Faqat `<h1>Welcome to Task Center</h1>` yozuvi bor. Bu yerda foydalanuvchining faol loyihalari, unga biriktirilgan vazifalar va qisqacha statistika bo'lishi maqsadga muvofiq.

### 3.3. Profil sahifasida qotirilgan (Hardcoded) ma'lumotlar
* **Fayl:** `src/widgets/profile/index.tsx`
* `<ProfileStatus taskCount={20} />` — vazifalar soni har doim 20 ta deb ko'rsatiladi. Real backend statistikasiga ulanmagan.

### 3.4. API Map va Naming xatolari
* `src/shared/api/apiMap.ts` da imlo xatosi: `WOKSPACE_GET_ALL_MEMBERS` (`WORKSPACE` o'rniga `WOKSPACE`).
* `src/entities/workspaces/api/index.ts` da dublikat: `deleteWorkspace` va `deleteWorspace`.

---

## 🛠️ AMALIY TO'G'RILASH REJASI VA KOD NAMUNALARI

### 1-qadam: `src/shared/api/baseApi.ts` ni to'g'rilash (BaseURL, 401 interceptor, TMA initData)
```typescript
import type { BaseQueryApi, FetchArgs } from "@reduxjs/toolkit/query/react";
import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";

const getBaseUrl = () => {
  const envUrl = import.meta.env.VITE_APP_BASE_URL;
  if (!envUrl) return "http://localhost:8080/api";
  return envUrl.endsWith("/api") ? envUrl : `${envUrl}/api`;
};

const rawBaseQuery = fetchBaseQuery({
  baseUrl: getBaseUrl(),
  prepareHeaders: (headers) => {
    const token = localStorage.getItem("accessToken") || localStorage.getItem("token");
    if (token && !headers.has("Authorization")) {
      headers.set("Authorization", `Bearer ${token}`);
    }
    // Telegram Mini App initData
    const tgInitData = (window as unknown as { Telegram?: { WebApp?: { initData?: string } } })
      ?.Telegram?.WebApp?.initData;
    if (tgInitData) {
      headers.set("X-Telegram-Init-Data", tgInitData);
    }
    return headers;
  },
});

const baseQueryWithReauth = async (args: string | FetchArgs, api: BaseQueryApi, extraOptions: Record<string, unknown>) => {
  const result = await rawBaseQuery(args, api, extraOptions);
  if (result.error && result.error.status === 401) {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("token");
    window.location.href = "/login";
  }
  return result;
};

export const baseApi = createApi({
  reducerPath: "baseApi",
  baseQuery: baseQueryWithReauth,
  refetchOnReconnect: true,
  endpoints: () => ({}),
  tagTypes: ["Workspace", "Workspace-columns", "Workspace-members", "Tasks"],
});
```

### 2-qadam: Task API (`src/entities/tasks/api/index.ts`) yaratish
```typescript
import { API_METHODS } from "@/shared/api/api-metods";
import { baseApi } from "@/shared/api/baseApi";

export const taskApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    createTask: builder.mutation({
      query: ({ workspaceId, data }: { workspaceId: string; data: any }) => ({
        url: `/workspaces/${workspaceId}/tasks`,
        method: API_METHODS.POST,
        body: data,
      }),
      invalidatesTags: ["Workspace-columns", "Tasks"],
    }),
    reorderTask: builder.mutation({
      query: ({ workspaceId, taskId, data }: { workspaceId: string; taskId: string; data: { columnId?: string; prevRank?: string; nextRank?: string } }) => ({
        url: `/workspaces/${workspaceId}/tasks/${taskId}/reorder`,
        method: API_METHODS.PATCH,
        body: data,
      }),
      invalidatesTags: ["Workspace-columns", "Tasks"],
    }),
    deleteTask: builder.mutation({
      query: ({ workspaceId, taskId }: { workspaceId: string; taskId: string }) => ({
        url: `/workspaces/${workspaceId}/tasks/${taskId}`,
        method: API_METHODS.DELETE,
      }),
      invalidatesTags: ["Workspace-columns", "Tasks"],
    }),
  }),
});

export const { useCreateTaskMutation, useReorderTaskMutation, useDeleteTaskMutation } = taskApi;
```

### 3-qadam: Kanban Board Drag-and-Drop ga `reorderTask` ni ulash
`kanban-board.tsx` faylida vazifa surilganda (170-189-qatorlar) `useReorderTaskMutation` orqali quyidagi so'rov yuborilishi shart:
```typescript
await reorderTask({
  workspaceId,
  taskId: dragged.id,
  data: {
    columnId: targetLoc.colId,
    prevRank: targetTasks[targetLoc.index - 1]?.rank,
    nextRank: targetTasks[targetLoc.index + 1]?.rank,
  },
}).unwrap();
```

---

## 📊 Xulosa Jadvali (Summary Matrix)

| Modul | Holat | Xatolik darajasi | Tavsif |
| :--- | :---: | :---: | :--- |
| **Auth (Login/Register)** | ⚠️ Qisman ishlaydi | Medium | Register/Login ishlaydi, lekin 401 redirect va token tozalashda kamchilik bor |
| **Workspace List** | ✅ Ishlaydi | Low | Yangi workspace yaratish, ro'yxatni ko'rish va o'chirish ishlaydi |
| **Workspace Columns** | ✅ Ishlaydi | Low | Ustun qo'shish, ustunlarni surish (reorder) ishlaydi |
| **Workspace Members** | ⚠️ Qisman ishlaydi | Medium | A'zolarni taklif qilish va rolni o'zgartirish bor, xatolik xabarlari to'g'rilanishi lozim |
| **Kanban Tasks Board** | ❌ Ishlamaydi | **CRITICAL** | Vazifalar yuklanmaydi, DND backend ga saqlanmaydi, yangi vazifa qo'shish API ga ulanmagan |
| **Telegram Mini App** | ❌ Mavjud emas | High | Mini App muhiti inobatga olinmagan, WebApp auto-auth qilinmagan |
