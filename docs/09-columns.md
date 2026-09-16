# 09. Column'lar

`BoardColumn` entity (`board_columns` jadvali): UUID `id`, `workspaceId`, `title`,
`order` (ustun nomi `column_order`), `isDefault`, audit ustunlari, soft delete.
`tasks` maydoni board view ishlatadigan lazy `@OneToMany(mappedBy = "columnId")`.

## Endpoint'lar (`BoardController`)

Hamma endpoint `checkOwnerOrAdmin` talab qiladi, faqat ro'yxat `checkAccess` bilan
ochiq:

| Metod | Path | Nima qiladi |
|---|---|---|
| GET | `/api/workspaces/{wid}/columns` | `checkAccess`. Order bo'yicha ro'yxat (`ColumnDto{id, workspaceId, title, order, isDefault}`) |
| POST | `/api/workspaces/{wid}/columns` | Body `ColumnCreateRequest{title*, order}`. `order` berilmasa yoki 0 dan kichik bo'lsa `max+1` qo'yiladi. `COLUMN_CREATED` publish qiladi |
| PUT | `/api/workspaces/{wid}/columns/{id}` | Null bo'lmagan `title`/`order` ni yangilaydi. `COLUMN_UPDATED` publish qiladi |
| PATCH | `/api/workspaces/{wid}/columns/{id}` | PUT bilan bir xil partial yangilash. `COLUMN_UPDATED` publish qiladi |
| PATCH | `/api/workspaces/{wid}/columns` | Reorder, body `[{id, order}]`. Hamma id shu workspace'ga tegishli bo'lishi shart, aks holda 404. Event chiqmaydi |
| DELETE | `/api/workspaces/{wid}/columns/{id}` | Qatorni o'chiradi. `COLUMN_DELETED` publish qiladi |

`title` bo'sh bo'lmasin va 255 belgidan oshmasin (POST/PUT body ko'rsatilgan joyda
`@Valid`). Boshqa workspace'ning column id'si berilsa `EntityNotFoundException`
(404) chiqadi. Hard delete bo'lsa, DB cascade (`fk_tasks_column ON DELETE CASCADE`)
column tasklarini o'chiradi.
