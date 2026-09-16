# 17. Keshlash

`CacheConfig` orqali Caffeine (`@EnableCaching`). Bitta umumiy builder hamma kesh
uchun: ko'pi bilan 1000 yozuv, 5 daqiqalik write expiry, statistika yozib boriladi.
Kesh nomlari: `users`, `workspaces`, `workspaces-by-user`, `workspaces-by-id`,
`workspaceMembers`, `labels`, `columns`.

## Qaysi operatsiyalar keshlangan

| Kesh | Operatsiya | Qachon tozalanadi |
|---|---|---|
| `users` | `AuthService.getUserByName` (`@Cacheable`) | `register` shu nomni tozalaydi |
| `workspaces-by-user` | `WorkspaceService.getWorkspaceList` (kalit `userId-page-size`) | `createWorkspace` hammasini, `update/deleteWorkspace` hammasini |
| `workspaces-by-id` | `WorkspaceService.getWorkspaceById` | `update/deleteWorkspace` shu id ni |

## Nega board keshlanmaydi

`BoardService` da kesh annotatsiyasi yo'q, board/task/column o'qishlar doim DB ga
boradi. Shuning uchun real-time broadcast hech qachon eskirgan data bermaydi.

## Ishlatilmaydigan kesh nomlari

`workspaces`, `workspaceMembers`, `labels`, `columns` ro'yxatda bor, lekin
o'qilmaydi. Zararsiz, kelajak uchun zaxira turibdi.

## Audit

JPA auditing (`@EnableJpaAuditing` + `SecurityAuditorAware`) xuddi shu config
sinfida `createdBy` va `updatedBy` ni security context'dan to'ldiradi.
