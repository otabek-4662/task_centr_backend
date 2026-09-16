# 10. Tasklar

`Task` entity (`tasks` jadvali): UUID `id`, global unikal `publicId`, `workspaceId`,
`columnId`, `title`, `description` (TEXT), `order` (ustun nomi `task_order`),
`priority` (`LOW`/`MEDIUM`/`HIGH`/`URGENT`, default `MEDIUM`), `assigneeId` (oddiy
nullable id, FK emas), `dueDate`, audit ustunlari, soft delete. Label'lar bilan
`task_labels` orqali, userlar bilan `task_assignees` orqali many-to-many bog'langan.
Taskda `status` maydoni yo'q, holat column pozitsiyasi va priority orqali bilinadi.

## Yaratish (`POST /api/workspaces/{wid}/tasks`)

`checkAccess` shart. `BoardService.createTask` quyidagicha ishlaydi:
1. Column shu workspace'ga tegishli ekanini tekshiradi.
2. `order` berilmasa yoki 0 dan kichik bo'lsa, shu column'dagi `MAX(order)+1` olinadi.
3. Workspace qatori locklanadi (`findByIdForUpdate`, pessimistic write),
   `taskCounter` bittaga oshirilib saqlanadi.
4. `publicId = keyPrefix + "-" + counter` yasaladi (masalan `TC-6`).
5. `assigneeId` berilgan bo'lsa, u user mavjud va workspace a'zosi yoki egasi
   bo'lishi shart, aks holda 404/403 chiqadi.
6. Saqlanadi, `TaskDto` qaytadi, `TASK_CREATED` publish qilinadi.

So'rov (`TaskCreateRequest`): `columnId*`, `title*` (255 belgigacha), `description`
(2000 belgigacha), `order`, `priority` (default `MEDIUM`), `assigneeId`, `dueDate`
(`@FutureOrPresent`).

## Yangilashlar

- `PUT`/`PATCH /api/workspaces/{wid}/tasks/{id}` (`TaskUpdateRequest`, hamma maydon
  ixtiyoriy): faqat null bo'lmagan maydonlar yangilanadi, `columnId` orqali task'ni
  boshqa column'ga ko'chirish ham shu yerdan bo'ladi. `TASK_UPDATED` publish qiladi.
  `publicId` hech qachon o'zgarmaydi.
- `PATCH .../tasks/{taskId}/column/{columnId}` column'lararo ko'chiradi. Faqat
  saqlaydi, WebSocket event chiqmaydi (`updateTask` dan farqi shu).
- `PUT .../tasks/{taskId}/assignee/{userId}` yagona `assigneeId` ni almashtiradi
  (user a'zo yoki ega bo'lishi shart). Event chiqmaydi.

## Task'dagi labellar

- `POST .../tasks/{taskId}/labels/{labelId}` va `DELETE ...` label'ni qo'shadi va
  olib tashlaydi (label shu workspace'ga tegishli bo'lishi shart). Event chiqmaydi.

## O'chirish va o'qish

- `DELETE .../tasks/{id}` soft delete qiladi, `TASK_DELETED` publish qiladi.
- `GET .../tasks?page=&size=`: page yoki size manfiy bo'lsa to'liq ro'yxat, aks
  holda page'li (ko'pi bilan 100). `GET .../tasks/{id}` label va assignee'lar bilan
  tafsilot qaytaradi.

## Parallel yozuv

Counter oshirish workspace pessimistic lock bilan himoyalangan, shuning uchun bitta
workspace'da parallel `createTask` chaqiruvlari dublikat `publicId` bera olmaydi.
Batafsil 19-bo'limda.
