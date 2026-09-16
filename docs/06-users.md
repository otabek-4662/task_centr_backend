# 06. Foydalanuvchilar

User entity'da UUID `id`, unikal `name`, `fullName` (default `name`), unikal `email`
(default `user_<id>@taskcenter.local`), BCrypt `password`, `role` (`USER`/`ADMIN`,
default `USER`), audit ustunlari va soft delete (`deleted_at`, `@Where` bilan
filtrlanadi) bor. Spring `UserDetails` ni implement qiladi (authority `ROLE_<role>`).

## `GET /api/me` va `GET /api/auth/me`

Autentifikatsiya shart. Chaqiruvchining `UserDto{id, name, fullName}` qaytadi.
`currentUser` null bo'lsa `RuntimeException("Unauthorized")` chiqib, umumiy handler
orqali 500 bo'ladi, lekin autentifikatsiya ortida bunga amalda erishib bo'lmaydi.

## `GET /api/me/stats`

Autentifikatsiya shart. `UserStatsDto{taskCount, workspaceCount}` qaytaradi:
`taskCount` shu userga biriktirilgan tasklar soni
(`TaskRepository.countByAssigneeId`), `workspaceCount` egalik qilgan yoki a'zo
bo'lgan workspace'lar soni (`WorkspaceRepository.countByOwnerIdOrMemberUserId`).
Profil sahifasi hardcode o'rniga shundan foydalanadi.

## `GET /api/users`

Autentifikatsiya shart, ustiga `ADMIN` ilova roli kerak (bo'lmasa 403).
`workspaceId` siz chaqirilsa hamma user `userRepository.findAll(pageable)` orqali
keladi (default size 20, `name` bo'yicha sort). `workspaceId` berilsa, oldin shu
workspace uchun `checkAccess` o'tadi, keyin a'zolar ro'yxati qo'lda paginate
qilinadi (`findByWorkspaceId`, keyin `findAllById`, keyin sublist). Javob Spring
`Page<UserDto>`.
