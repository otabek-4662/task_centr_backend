# 07. Workspace'lar

Workspace entity'da UUID `id`, `title`, `bgColor` (255 belgigacha ixtiyoriy string),
`description`, `ownerId`, `keyPrefix` (10 belgigacha, aktiv workspace'lar orasida
global unikal), `taskCounter`, audit ustunlari va soft delete bor.

## Yaratish (`POST /api/workspaces`)

Istalgan autentifikatsiyalangan user yarata oladi. `WorkspaceService.createWorkspace`
qatorni `ownerId = chaqiruvchi` bilan quradi, key prefiks generatsiya qiladi,
saqlaydi, keyin `OWNER` membership qatorini yozadi.

### Key prefiks qanday yasaladi

`generateKeyPrefix(title)` title bo'sh bo'lsa `"WS"` qaytaradi, bitta so'z bo'lsa
boshidan ko'pi bilan 4 belgini katta harfda oladi, ko'p so'z bo'lsa bosh harflarni
oladi (ko'pi bilan 4 ta), masalan `"Website Redesign"` dan `"WR"` chiqadi.
`resolveUniqueKeyPrefix` `existsByKeyPrefix` tekkancha oxiriga `2`, `3` ... qo'shib
boradi (10 belgi limitiga sig'dirib boshini kesadi), shuning uchun bir xil nomlar
hech qachon to'qnashmaydi (`WR`, `WR2`).

Unikallik ikki joyda ta'minlanadi: generatsiyadagi tekshir-tanla sikli va partial DB
unique index `uk_workspace_key_prefix ... WHERE deleted_at IS NULL` (V10,
o'chirilgan qatorlar prefiksni qayta ishlatishga to'sqinlik qilmaydi). Qolgan race
holati haqida 19-bo'limda yozilgan.

## A'zolar

- `POST /api/workspaces/{id}/members?userId=` (`checkOwnerOrAdmin`) har doim `MEMBER`
  qator yaratadi. Kodda mavjudlik tekshiruvi yo'q, shuning uchun dublikat composite
  PK ga urilib 409 `DataIntegrityViolationException` beradi.
- `DELETE /api/workspaces/{id}/members/{userId}` (`checkOwnerOrAdmin`) membership
  qatori topilmasa 404 qaytaradi.
- `GET /api/workspaces/{id}/members` (`checkAccess`) a'zolarni `UserDto` ro'yxat
  qilib qaytaradi.

## Yangilash va o'chirish

- `PUT /api/workspaces/{id}` (`checkOwner`): faqat null bo'lmagan `title`/`bgColor`/
  `description` yangilanadi. `keyPrefix` yaratilgach o'zgarmaydi.
- `DELETE /api/workspaces/{id}` (`checkOwner`): soft delete (`deleted_at`). Hard
  delete bo'lsa, child qatorlar DB darajasida cascade bo'ladi (V4 `ON DELETE CASCADE`).

## Ro'yxat

`GET /api/workspaces?page=&size=`: `page < 0 || size <= 0` bo'lsa to'liq ro'yxat
qaytadi, aks holda page'li (size ko'pi bilan 100). Natijalar keshlanadi
(`workspaces-by-user`), create/update/delete keshlarni tozalaydi.
