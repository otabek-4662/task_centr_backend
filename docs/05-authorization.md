# 05. Avtorizatsiya

Bu loyihada ikkita alohida rol tizimi bor, ularni adashtirmang.

## 1. Ilova rollari (`User.Role`)

`USER` va `ADMIN` rollari `users.role` da saqlanadi va JWT `role` claim'iga
qo'yiladi. HTTP darajada `SecurityConfig` da tekshiriladi:

| Qoida | Kim kira oladi |
|---|---|
| `GET /api/users`, `DELETE /api/users/**` | faqat `ADMIN` |
| `/api/workspaces/**` | `USER` yoki `ADMIN` |
| `POST /api/auth/register`, `/login`, Swagger, actuator health/metrics/prometheus | hamma (public) |
| qolgan hamma yo'l | istalgan autentifikatsiyalangan user |

## 2. Workspace rollari (`WorkspaceRole`)

`OWNER`, `ADMIN`, `MEMBER` rollari `workspace_members` da `(workspaceId, userId)`
juftligiga saqlanadi (composite PK + unique cheklov). Workspace yaratgan user
`createWorkspace` ichida yoziladigan owner-membership qatori orqali `OWNER` bo'ladi.

`WorkspaceAuthorizationService.getRole` rolni shunday aniqlaydi:
1. Workspace topilmasa yoki `userId` null bo'lsa, rol bo'sh qaytadi (ruxsat yo'q).
2. `workspace.ownerId == userId` bo'lsa, `OWNER` (membership qatori shart emas).
3. Qolgan holda membership qatoridagi rol olinadi, agar qator bo'lsa.

## Service darajadagi tekshiruvlar

| Metod | Qoida | Ruxsat bo'lmasa |
|---|---|---|
| `checkAccess` | `OWNER`, `ADMIN` yoki `MEMBER` | `ForbiddenException`, 403. Workspace yo'q bo'lsa oldin 404 chiqadi |
| `checkOwner` | faqat `OWNER` | 403 (`"Faqat workspace egasi …"`) |
| `checkOwnerOrAdmin` | `OWNER` yoki `ADMIN` | 403 (`"Faqat workspace egasi yoki admin …"`) |
| `isUserMemberOfWorkspace` | membership qatori borligi (owner hisoblanmaydi) | boolean qaytaradi |
| `isOwner` / `isOwnerOrAdmin` / `hasAccess` / `isWorkspaceAdminOrMember` | boolean yordamchilar, servislar bo'ylab ishlatiladi | boolean qaytaradi |

Bitta nozik joy bor: ayrim oqimlar ataylab
`isUserMemberOfWorkspace(...) || isOwner(...)` tekshiradi (masalan assignee
validatsiya). Sababi membership owner'ni qamrab olmaydi.

## Endpoint va tekshiruv xaritasi (workspace doirasida)

| Endpoint'lar | Kerakli tekshiruv |
|---|---|
| O'qish: column/task/board/label/member ro'yxatlar, task tafsiloti, `assignTaskToUser`, label qo'shish/olib tashlash, task column ko'chirish | `checkAccess` |
| Column/label yaratish/yangilash/o'chirish, workspace a'zo qo'shish/o'chirish | `checkOwnerOrAdmin` |
| Workspace yangilash/o'chirish | `checkOwner` |
| `GET /api/users?workspaceId=` | shu workspace uchun `checkAccess` |
| Task assignee mavjud va a'zo/owner bo'lishi shart | `validateAssigneeId`, aks holda 404/403 |
