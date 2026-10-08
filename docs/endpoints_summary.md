# OpenAPI Hujjati — Barcha Endpointlar va OperationId lar Ro'yxati

| # | Tag | HTTP Metod | Endpoint URL | OperationId | Tavsif | Holati |
|---|---|---|---|---|---|---|
| 1 | Attachments | `DELETE` | `/api/attachments/{attachmentId}` | `deleteTaskAttachment` | Biriktirilgan faylni o'chirish | ✅ Faol |
| 2 | Attachments | `GET` | `/api/attachments/{attachmentId}/download` | `downloadTaskAttachment` | Faylni yuklab olish yoki ko'rish | ✅ Faol |
| 3 | Attachments | `GET` | `/api/tasks/{taskId}/attachments` | `getTaskAttachments` | Taskka biriktirilgan barcha fayllar ro'yxatini olish | ✅ Faol |
| 4 | Attachments | `POST` | `/api/tasks/{taskId}/attachments` | `uploadTaskAttachment` | Taskka fayl yoki rasm yuklash | ✅ Faol |
| 5 | Auth | `POST` | `/api/auth/forgot-password` | `forgotPassword` | Parolni tiklash so'rovi (Email orqali havola yuborish) | ✅ Faol |
| 6 | Auth | `POST` | `/api/auth/login` | `loginUser` | Login - token beradi | ✅ Faol |
| 7 | Auth | `POST` | `/api/auth/logout` | `logoutUser` | Logout - sessiyani yakunlash | ✅ Faol |
| 8 | Auth | `POST` | `/api/auth/refresh` | `refreshToken` | Refresh - yangi token beradi | ✅ Faol |
| 9 | Auth | `POST` | `/api/auth/register` | `registerUser` | Register - token beradi | ✅ Faol |
| 10 | Auth | `POST` | `/api/auth/reset-password` | `resetPassword` | Yangi parol o'rnatish | ✅ Faol |
| 11 | Auth | `POST` | `/api/v1/auth/forgot-password` | `v1ForgotPassword` | Parolni tiklash so'rovi (eski v1 endpoint) | ⚠️ Deprecated |
| 12 | Auth | `POST` | `/api/v1/auth/login` | `v1LoginUser` | Login (eski v1 endpoint) | ⚠️ Deprecated |
| 13 | Auth | `POST` | `/api/v1/auth/logout` | `v1LogoutUser` | Logout (eski v1 endpoint) | ⚠️ Deprecated |
| 14 | Auth | `POST` | `/api/v1/auth/refresh` | `v1RefreshToken` | Refresh token (eski v1 endpoint) | ⚠️ Deprecated |
| 15 | Auth | `POST` | `/api/v1/auth/register` | `v1RegisterUser` | Register (eski v1 endpoint) | ⚠️ Deprecated |
| 16 | Auth | `POST` | `/api/v1/auth/reset-password` | `v1ResetPassword` | Yangi parol o'rnatish (eski v1 endpoint) | ⚠️ Deprecated |
| 17 | Board | `GET` | `/api/workspaces/{workspaceId}/board` | `getKanbanBoard` | Workspace doskasini barcha ustunlar va ularning vazifalari bilan olish | ✅ Faol |
| 18 | Chat | `GET` | `/api/chat/conversations` | `getDirectChatConversations` | DM suhbatlar ro'yxati (oxirgi xabar + unread count) | ✅ Faol |
| 19 | Chat | `POST` | `/api/chat/direct` | `sendDirectChatMessage` | Shaxsiy (DM) xabar yuborish | ✅ Faol |
| 20 | Chat | `GET` | `/api/chat/direct/{otherUserId}` | `getDirectChatMessages` | Foydalanuvchi bilan shaxsiy yozishmalar tarixini olish | ✅ Faol |
| 21 | Chat | `POST` | `/api/chat/direct/{otherUserId}/read-all` | `markAllDirectChatMessagesAsRead` | Suhbatdagi barcha xabarlarni o'qilgan deb belgilash | ✅ Faol |
| 22 | Chat | `GET` | `/api/chat/direct/{otherUserId}/search` | `searchDirectChatMessages` | DM suhbatdan xabar qidirish | ✅ Faol |
| 23 | Chat | `PUT` | `/api/chat/messages/{messageId}` | `editChatMessage` | Xabarni tahrirlash | ✅ Faol |
| 24 | Chat | `DELETE` | `/api/chat/messages/{messageId}` | `deleteChatMessage` | Xabarni o'chirish (hammadan) | ✅ Faol |
| 25 | Chat | `POST` | `/api/chat/messages/{messageId}/read` | `markChatMessageAsRead` | Xabarni o'qilgan deb belgilash | ✅ Faol |
| 26 | Chat | `GET` | `/api/chat/online` | `getOnlineChatUsers` | Hozir online foydalanuvchilar ro'yxati | ✅ Faol |
| 27 | Chat | `GET` | `/api/chat/public` | `getPublicChatMessages` | Umumiy chat xabarlarini sahifalab olish | ✅ Faol |
| 28 | Chat | `POST` | `/api/chat/public` | `sendPublicChatMessage` | Umumiy chatga xabar yuborish | ✅ Faol |
| 29 | Chat | `GET` | `/api/chat/public/search` | `searchPublicChatMessages` | Umumiy chatdan xabar qidirish | ✅ Faol |
| 30 | Chat | `GET` | `/api/chat/search` | `searchAllChatMessages` | Barcha ko'rinadigan xabarlar orasidan qidirish (Public + DM) | ✅ Faol |
| 31 | Chat | `GET` | `/api/chat/users` | `getChatUsers` | Chat uchun foydalanuvchilar ro'yxati (online holati bilan) | ✅ Faol |
| 32 | Chat | `POST` | `/api/chat/{messageId}/reactions` | `toggleChatMessageReaction` | Xabarga reaksiya qo'shish yoki olib tashlash (toggle) | ✅ Faol |
| 33 | Checklists | `GET` | `/api/tasks/{taskId}/checklists` | `getTaskChecklists` | Taskning barcha checklistlarini olish | ✅ Faol |
| 34 | Checklists | `POST` | `/api/tasks/{taskId}/checklists` | `createTaskChecklistItem` | Yangi checklist bandi qo'shish | ✅ Faol |
| 35 | Checklists | `PUT` | `/api/tasks/{taskId}/checklists/{itemId}` | `updateTaskChecklistItem` | Checklist bandini yangilash (sarlavha yoki bajarilganlik) | ✅ Faol |
| 36 | Checklists | `DELETE` | `/api/tasks/{taskId}/checklists/{itemId}` | `deleteTaskChecklistItem` | Checklist bandini o'chirish | ✅ Faol |
| 37 | Checklists | `GET` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists` | `getWorkspaceTaskChecklistsLegacy` | Taskning barcha checklistlarini olish (eski uslub) | ⚠️ Deprecated |
| 38 | Checklists | `POST` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists` | `createWorkspaceTaskChecklistItemLegacy` | Yangi checklist qo'shish (eski uslub) | ⚠️ Deprecated |
| 39 | Checklists | `PUT` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists/{itemId}` | `updateWorkspaceTaskChecklistItemLegacy` | Checklistni yangilash (eski uslub) | ⚠️ Deprecated |
| 40 | Checklists | `DELETE` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists/{itemId}` | `deleteWorkspaceTaskChecklistItemLegacy` | Checklistni o'chirish (eski uslub) | ⚠️ Deprecated |
| 41 | Columns | `GET` | `/api/workspaces/{workspaceId}/columns` | `getWorkspaceColumns` | Workspace ga tegishli ustunlar ro'yxatini olish | ✅ Faol |
| 42 | Columns | `POST` | `/api/workspaces/{workspaceId}/columns` | `createWorkspaceColumn` | Yangi ustun yaratish | ✅ Faol |
| 43 | Columns | `PATCH` | `/api/workspaces/{workspaceId}/columns` | `reorderWorkspaceColumnsLegacy` | Ustunlar tartibini o'zgartirish (eski variant) | ⚠️ Deprecated |
| 44 | Columns | `PATCH` | `/api/workspaces/{workspaceId}/columns/reorder` | `reorderWorkspaceColumns` | Ustunlar tartibini ID lar ro'yxati orqali yangilash (oddiy massiv: ['col1', 'col2']) | ✅ Faol |
| 45 | Columns | `PUT` | `/api/workspaces/{workspaceId}/columns/{id}` | `updateWorkspaceColumn` | Ustunni to'liq yangilash | ✅ Faol |
| 46 | Columns | `DELETE` | `/api/workspaces/{workspaceId}/columns/{id}` | `deleteWorkspaceColumn` | Ustunni o'chirish | ✅ Faol |
| 47 | Columns | `PATCH` | `/api/workspaces/{workspaceId}/columns/{id}` | `patchWorkspaceColumn` | Ustunni qisman yangilash | ✅ Faol |
| 48 | Comments | `GET` | `/api/tasks/{taskId}/comments` | `getTaskComments` | Task izohlari ro'yxatini sahifalab olish | ✅ Faol |
| 49 | Comments | `POST` | `/api/tasks/{taskId}/comments` | `createTaskComment` | Taskka yangi izoh qo'shish | ✅ Faol |
| 50 | Comments | `PUT` | `/api/tasks/{taskId}/comments/{commentId}` | `updateTaskComment` | Izohni tahrirlash | ✅ Faol |
| 51 | Comments | `DELETE` | `/api/tasks/{taskId}/comments/{commentId}` | `deleteTaskComment` | Izohni o'chirish | ✅ Faol |
| 52 | Config | `GET` | `/api/v1/config/public` | `getPublicSystemConfig` | Get public config | ✅ Faol |
| 53 | Directions | `GET` | `/api/workspaces/{workspaceId}/directions` | `getWorkspaceDirections` | Workspace ga tegishli barcha yo'nalishlarni olish | ✅ Faol |
| 54 | Directions | `POST` | `/api/workspaces/{workspaceId}/directions` | `createWorkspaceDirection` | Yangi yo'nalish qo'shish | ✅ Faol |
| 55 | Directions | `PUT` | `/api/workspaces/{workspaceId}/directions/{id}` | `updateWorkspaceDirection` | Yo'nalishni tahrirlash (nomi, rangi) | ✅ Faol |
| 56 | Directions | `DELETE` | `/api/workspaces/{workspaceId}/directions/{id}` | `deleteWorkspaceDirection` | Yo'nalishni o'chirish (Faqat OWNER/ADMIN) | ✅ Faol |
| 57 | Export | `GET` | `/api/workspaces/{workspaceId}/export/csv` | `exportWorkspaceTasksToCsv` | Workspace dagi barcha vazifalarni CSV formatida yuklash | ✅ Faol |
| 58 | Fayllar (Files) | `POST` | `/api/files/upload` | `uploadGeneralFile` | Fayl yuklash (xabar biriktirmasi uchun) | ✅ Faol |
| 59 | Fayllar (Files) | `GET` | `/api/files/{fileName}` | `downloadGeneralFile` | Faylni yuklab olish yoki ko'rish | ✅ Faol |
| 60 | Labels | `GET` | `/api/workspaces/{workspaceId}/labels` | `getWorkspaceLabels` | Workspace ga tegishli barcha labellarni olish | ✅ Faol |
| 61 | Labels | `POST` | `/api/workspaces/{workspaceId}/labels` | `createWorkspaceLabel` | Yangi label yaratish | ✅ Faol |
| 62 | Labels | `PUT` | `/api/workspaces/{workspaceId}/labels/{id}` | `updateWorkspaceLabel` | Label ni yangilash | ✅ Faol |
| 63 | Labels | `DELETE` | `/api/workspaces/{workspaceId}/labels/{id}` | `deleteWorkspaceLabel` | Label ni o'chirish | ✅ Faol |
| 64 | Notifications | `GET` | `/api/notifications` | `getMyNotifications` | Mening bildirishnomalarim ro'yxatini olish (Pageable) | ✅ Faol |
| 65 | Notifications | `PUT` | `/api/notifications/read-all` | `markAllNotificationsAsRead` | Barcha o'qilmaganlarni o'qilgan deb belgilash | ✅ Faol |
| 66 | Notifications | `PUT` | `/api/notifications/{id}/read` | `markNotificationAsRead` | Bildirishnomani o'qilgan deb belgilash | ✅ Faol |
| 67 | Reports | `GET` | `/api/workspaces/{workspaceId}/reports/sprints/{sprintId}` | `getSprintSummaryReport` | Muayyan Sprint bo'yicha ishlash tezligi (Burn-down) hisobotini olish | ✅ Faol |
| 68 | Reports | `GET` | `/api/workspaces/{workspaceId}/reports/summary` | `getWorkspaceSummaryReport` | Loyiha bo'yicha umumiy hisobot va xodimlar ish yukini olish | ✅ Faol |
| 69 | Sprints | `GET` | `/api/workspaces/{workspaceId}/backlog` | `getWorkspaceBacklog` | Workspace Backlog vazifalari ro'yxatini olish (sprintsiz tasklar) | ✅ Faol |
| 70 | Sprints | `GET` | `/api/workspaces/{workspaceId}/sprints` | `listWorkspaceSprints` | Workspacedagi sprintlar ro'yxatini olish (status bo'yicha filter qilish mumkin) | ✅ Faol |
| 71 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints` | `createWorkspaceSprint` | Yangi sprint yaratish | ✅ Faol |
| 72 | Sprints | `GET` | `/api/workspaces/{workspaceId}/sprints/{id}` | `getWorkspaceSprintById` | Sprint ma'lumotlarini id bo'yicha olish | ✅ Faol |
| 73 | Sprints | `PUT` | `/api/workspaces/{workspaceId}/sprints/{id}` | `updateWorkspaceSprint` | Sprintni tahrirlash | ✅ Faol |
| 74 | Sprints | `DELETE` | `/api/workspaces/{workspaceId}/sprints/{id}` | `deleteWorkspaceSprint` | Sprintni o'chirish (vazifalar backlogga qaytariladi) | ✅ Faol |
| 75 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints/{id}/complete` | `completeWorkspaceSprint` | Sprintni yakunlash (COMPLETED holatiga o'tkazish) | ✅ Faol |
| 76 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints/{id}/start` | `startWorkspaceSprint` | Sprintni boshlash (ACTIVE holatiga o'tkazish) | ✅ Faol |
| 77 | Sprints | `GET` | `/api/workspaces/{workspaceId}/sprints/{id}/tasks` | `getSprintTasks` | Sprintdagi barcha vazifalarni olish | ✅ Faol |
| 78 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints/{id}/tasks` | `addTasksToSprint` | Vazifalarni sprintga biriktirish | ✅ Faol |
| 79 | Sprints | `DELETE` | `/api/workspaces/{workspaceId}/sprints/{id}/tasks/{taskId}` | `removeTaskFromSprint` | Vazifani sprintdan chiqarib backlogga qaytarish | ✅ Faol |
| 80 | Task Activities | `GET` | `/api/tasks/{taskId}/activities` | `getTaskActivities` | Task o'zgarishlar tarixini sahifalab olish (History) | ✅ Faol |
| 81 | Tasks | `GET` | `/api/workspaces/{workspaceId}/tasks` | `listWorkspaceTasks` | Workspace ga tegishli vazifalar ro'yxatini olish (qidiruv, filter va pagination bilan) | ✅ Faol |
| 82 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks` | `createWorkspaceTask` | Yangi vazifa yaratish | ✅ Faol |
| 83 | Tasks | `GET` | `/api/workspaces/{workspaceId}/tasks/{id}` | `getWorkspaceTaskById` | Bitta vazifani ID orqali olish (batafsil ma'lumotlari bilan) | ✅ Faol |
| 84 | Tasks | `PUT` | `/api/workspaces/{workspaceId}/tasks/{id}` | `updateWorkspaceTask` | Vazifani to'liq yangilash | ✅ Faol |
| 85 | Tasks | `DELETE` | `/api/workspaces/{workspaceId}/tasks/{id}` | `deleteWorkspaceTask` | Vazifani o'chirish | ✅ Faol |
| 86 | Tasks | `PATCH` | `/api/workspaces/{workspaceId}/tasks/{id}` | `patchWorkspaceTask` | Vazifani qisman yangilash | ✅ Faol |
| 87 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/archive` | `archiveWorkspaceTask` | Vazifani arxivga olish | ✅ Faol |
| 88 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/assign` | `toggleTaskAssignee` | Vazifaga foydalanuvchini biriktirish / olib tashlash | ✅ Faol |
| 89 | Tasks | `PATCH` | `/api/workspaces/{workspaceId}/tasks/{id}/reorder` | `reorderWorkspaceTask` | Vazifaning o'rnini (rank) va ustunini yangilash (Lexorank) | ✅ Faol |
| 90 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/unarchive` | `unarchiveWorkspaceTask` | Vazifani arxivdan chiqarish | ✅ Faol |
| 91 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/watch` | `toggleTaskWatch` | Vazifani kuzatish / kuzatishni to'xtatish (Watch) | ✅ Faol |
| 92 | Tasks Direct | `GET` | `/api/tasks/{id}` | `getTaskDirectById` | Vazifani bevosita ID orqali olish (eski/dublikat endpoint) | ⚠️ Deprecated |
| 93 | Telegram Auth | `POST` | `/api/v1/auth/telegram` | `loginWithTelegram` | Telegram Mini App orqali login | ✅ Faol |
| 94 | Users | `GET` | `/api/auth/me` | `getCurrentUserLegacy` | Joriy foydalanuvchi profilini olish (eski alias) | ⚠️ Deprecated |
| 95 | Users | `GET` | `/api/me` | `getCurrentUser` | Joriy foydalanuvchi profilini olish | ✅ Faol |
| 96 | Users | `GET` | `/api/me/tasks` | `getMyAssignedTasksLegacy` | Joriy foydalanuvchiga biriktirilgan vazifalar (eski alias) | ⚠️ Deprecated |
| 97 | Users | `GET` | `/api/users` | `getWorkspaceUsers` | Workspace dagi foydalanuvchilar ro'yxatini olish | ✅ Faol |
| 98 | Users | `PATCH` | `/api/users/me` | `updateCurrentUserProfile` | Profil ma'lumotlarini tahrirlash | ✅ Faol |
| 99 | Users | `POST` | `/api/users/me/change-password` | `changePassword` | Parolni o'zgartirish | ✅ Faol |
| 100 | Users | `GET` | `/api/users/me/tasks` | `getMyAssignedTasks` | Joriy foydalanuvchiga biriktirilgan barcha vazifalar ro'yxatini olish (barcha workspacelar bo'yicha) | ✅ Faol |
| 101 | Users | `POST` | `/api/users/me/telegram` | `linkTelegram` | Mini App orqali Telegram akkauntni ulash | ✅ Faol |
| 102 | Users | `DELETE` | `/api/users/me/telegram` | `unlinkTelegram` | Telegram akkauntni uzish (faqat joriy foydalanuvchi uchun) | ✅ Faol |
| 103 | Users | `GET` | `/api/users/me/telegram-link-token` | `getTelegramLinkToken` | Telegram bilan ulash uchun vaqtinchalik token, tayyor havola va amal qilish muddatini olish | ✅ Faol |
| 104 | Workspace | `GET` | `/api/workspaces` | `getMyWorkspaces` | Foydalanuvchining workspace lari ro'yxatini olish | ✅ Faol |
| 105 | Workspace | `POST` | `/api/workspaces` | `createWorkspace` | Yangi workspace yaratish | ✅ Faol |
| 106 | Workspace | `GET` | `/api/workspaces/{id}` | `getWorkspaceById` | Bitta workspace ma'lumotlarini ID orqali olish | ✅ Faol |
| 107 | Workspace | `PUT` | `/api/workspaces/{id}` | `updateWorkspace` | Mavjud workspace ni tahrirlash | ✅ Faol |
| 108 | Workspace | `DELETE` | `/api/workspaces/{id}` | `deleteWorkspace` | Workspace ni o'chirish | ✅ Faol |
| 109 | Workspace Invitations | `GET` | `/api/invitations/me` | `getMyPendingInvitations` | Menga kelgan faol takliflarni ko'rish | ✅ Faol |
| 110 | Workspace Invitations | `PATCH` | `/api/invitations/{id}/accept` | `acceptWorkspaceInvitation` | Taklifni qabul qilish | ✅ Faol |
| 111 | Workspace Invitations | `PATCH` | `/api/invitations/{id}/reject` | `rejectWorkspaceInvitation` | Taklifni rad etish | ✅ Faol |
| 112 | Workspace Invitations | `GET` | `/api/workspaces/{workspaceId}/invites` | `getWorkspaceInvitations` | Workspace takliflarini ko'rish (Admin/Owner) | ✅ Faol |
| 113 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites` | `createWorkspaceInvitation` | Workspace ga taklif yuborish (Admin/Owner) | ✅ Faol |
| 114 | Workspace Invitations | `DELETE` | `/api/workspaces/{workspaceId}/invites/{id}` | `cancelWorkspaceInvitation` | Taklifni bekor qilish (Admin/Owner) | ✅ Faol |
| 115 | Workspace Members | `GET` | `/api/workspaces/{workspaceId}/members` | `getWorkspaceMembers` | Workspace a'zolari ro'yxatini rollari bilan olish | ✅ Faol |
| 116 | Workspace Members | `POST` | `/api/workspaces/{workspaceId}/members` | `inviteWorkspaceMember` | Workspace ga yangi a'zo taklif qilish / qo'shish | ✅ Faol |
| 117 | Workspace Members | `DELETE` | `/api/workspaces/{workspaceId}/members/{userId}` | `removeWorkspaceMember` | Workspace a'zosini chiqarib yuborish yoki jamoani tark etish | ✅ Faol |
| 118 | Workspace Members | `PATCH` | `/api/workspaces/{workspaceId}/members/{userId}` | `updateWorkspaceMemberRole` | Workspace a'zosining rolini o'zgartirish | ✅ Faol |
