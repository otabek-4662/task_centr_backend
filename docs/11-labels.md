# 11. Labellar

`Label` entity (`labels` jadvali): UUID `id`, `workspaceId`, `name`, `color`.
Uchalasi ham null bo'la olmaydi. Audit ustunlari va soft delete bor. Labellar doim
workspace doirasida bo'ladi, task bilan `task_labels` join jadvali orqali bog'lanadi
(composite PK, ikki tomonga cascade).

## Endpoint'lar (`BoardController`)

Hammasi `checkOwnerOrAdmin` talab qiladi:

| Metod | Path | Nima qiladi |
|---|---|---|
| GET | `/api/workspaces/{wid}/labels` | `LabelDto{id, workspaceId, name, color}` ro'yxati |
| POST | `/api/workspaces/{wid}/labels` | Body `LabelDto` dan `name` va `color` olinadi. `LABEL_CREATED` publish qiladi |
| DELETE | `/api/workspaces/{wid}/labels/{id}` | Label shu workspace'ga tegishli ekanini tekshirib o'chiradi. `LABEL_DELETED` publish qiladi |

`POST` da `@Valid` yo'q, shuning uchun bo'sh nom API darajasida o'tib ketadi.
Label'ni taskga qo'shish va olib tashlash 10-bo'limda.
