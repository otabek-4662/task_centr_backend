# 08. Boardlar

Board uchun alohida entity va jadval yo'q. U workspace column'lari va ularning
task'laridan yig'iladigan o'qish view'i.

## `GET /api/workspaces/{workspaceId}/board`

Autentifikatsiya va `checkAccess` shart. Column'lar tasklari bilan bitta so'rovda
yuklanadi (`ColumnRepository.findByWorkspaceIdWithTasks`, `@EntityGraph` orqali
`tasks`, `tasks.labels` va `tasks.assignees`), har column tasklari xotirada `order`
bo'yicha sortlanib `ColumnWithCardsDto{id, title, order, cards[]}` ga map qilinadi.
Har bir card `TaskCardDto{id, publicId, title, order}`. Standart workspace access
tekshiruvdan boshqa validatsiya yo'q, paginatsiya ham yo'q.

## Obyektlar qanday bog'langan

```
User ──(OWNER/ADMIN/MEMBER)──▶ Workspace ──▶ Board (view)
                                                      ├──▶ Column'lar (tartibli)
                                                      │         └──▶ Tasklar (tartibli)
                                                      └──▶ Label'lar, A'zolar
```

Frontend kanban ekranlar `/columns` va `/tasks` ni qo'lda yig'ishtirish o'rniga
shu endpoint'dan foydalanadi.
