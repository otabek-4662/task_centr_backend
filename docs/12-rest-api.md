# 12. REST API ma'lumotnomasi

Swagger base URL ni avtomatik aniqlaydi (`http://localhost:8080` yoki Render URL).
Har bir javob `ApiResponse{success, message, data, timestamp}` o'ramida qaytadi.
Jadvallardagi Auth ustuni: `-` hamma uchun ochiq, `U` kirgan user, `M` workspace
a'zosi (`checkAccess`), `O/A` owner yoki admin, `O` owner, `A` ilova admini.

## Auth - `AuthController` (`/api/auth`)

| Metod | Path | Body | Kodlar | Auth |
|---|---|---|---|---|
| POST | `/api/auth/register` | `{name*, password* min 6}` va javobda `{token, user}` | 201, 400, 409 (nom band) | - |
| POST | `/api/auth/login` | `{name*, password*}` va javobda `{token, user}` | 200, 401 | - |

## Userlar - `UserController` (`/api`)

| Metod | Path | Izoh | Auth |
|---|---|---|---|
| GET | `/api/me`, `/api/auth/me` | `UserDto{id, name, fullName}` | U |
| GET | `/api/me/stats` | `UserStatsDto{taskCount, workspaceCount}` | U |
| GET | `/api/users?workspaceId=&page=&size=` | Page'li `UserDto`. `workspaceId` berilsa shu workspace a'zoligi shart | A (filtrsiz) / M |

## Workspace'lar - `WorkspaceController` (`/api/workspaces`)

| Metod | Path | Body / param | Kodlar | Auth |
|---|---|---|---|---|
| GET | `/api/workspaces?page=&size=` | page yoki size manfiy bo'lsa to'liq ro'yxat, size ko'pi bilan 100 | 200 | U |
| POST | `/api/workspaces` | `{title*, bgColor 255 belgigacha, description 5000 belgigacha}` | 200, 400 | U |
| GET | `/api/workspaces/{id}` | - | 200, 404 | M |
| PUT | `/api/workspaces/{id}` | qisman `{title, bgColor, description}`, prefix o'zgarmaydi | 200, 404 | O |
| DELETE | `/api/workspaces/{id}` | soft delete | 200, 404 | O |
| POST | `/api/workspaces/{wid}/members?userId=` | har doim `MEMBER` yozadi, dublikat 409 beradi | 200, 404, 409 | O/A |
| DELETE | `/api/workspaces/{wid}/members/{userId}` | - | 200, 404 | O/A |

## Board / column / task / label / member - `BoardController` (`/api`)

| Metod | Path | Izoh | Auth |
|---|---|---|---|
| GET | `/workspaces/{wid}/board` | card'li column'lar (`ColumnWithCardsDto`) | M |
| GET/POST | `/workspaces/{wid}/columns` | ro'yxat / yaratish `{title*, order}` (order berilmasa yoki 0 dan kichik bo'lsa max+1) | M / O/A |
| PUT/PATCH | `/workspaces/{wid}/columns/{id}` | null bo'lmagan maydonlarni yangilash | O/A |
| PATCH | `/workspaces/{wid}/columns` | reorder `[{id, order}]`, hamma id shu workspace'ga tegishli bo'lishi shart | O/A |
| DELETE | `/workspaces/{wid}/columns/{id}` | - | O/A |
| GET | `/workspaces/{wid}/tasks?page=&size=` | page yoki size manfiy bo'lsa to'liq ro'yxat, ko'pi bilan 100 | M |
| GET | `/workspaces/{wid}/tasks/{id}` | label/assignee bilan tafsilot | M |
| POST | `/workspaces/{wid}/tasks` | `{columnId*, title*, description, order, priority, assigneeId, dueDate}` | M |
| PUT/PATCH | `/workspaces/{wid}/tasks/{id}` | null bo'lmagan maydonlarni patch (ko'chirish ham) | M |
| DELETE | `/workspaces/{wid}/tasks/{id}` | soft delete | M |
| PATCH | `/workspaces/{wid}/tasks/{tid}/column/{cid}` | ko'chirish (WS event chiqmaydi) | M |
| PUT | `/workspaces/{wid}/tasks/{tid}/assignee/{uid}` | biriktirish, a'zo/ega shart (event chiqmaydi) | M |
| POST/DELETE | `/workspaces/{wid}/tasks/{tid}/labels/{lid}` | label qo'shish/olib tashlash (event chiqmaydi) | M |
| GET/POST | `/workspaces/{wid}/labels` | ro'yxat / yaratish `{name, color}` | M / O/A |
| DELETE | `/workspaces/{wid}/labels/{id}` | - | O/A |
| GET | `/workspaces/{wid}/members` | a'zolar `UserDto` ro'yxati | M |

## Tizim

| Metod | Path | Auth |
|---|---|---|
| GET | `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` | - |
| GET | `/swagger-ui/index.html`, `/v3/api-docs` | - |

Eng ko'p uchraydigan xatolar: 400 validatsiya (`MethodArgumentNotValid`,
`IllegalArgumentException`), 401 noto'g'ri parol, 403 ruxsat yo'q yoki a'zo emas,
404 noma'lum yoki begona workspace id'si, 409 dublikat nom / membership / unique
buzilishi, 429 rate limit, 500 umumiy (`"Serverda xatolik yuz berdi…"` xabari,
tafsilot faqat server logida).
