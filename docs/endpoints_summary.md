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
| 18 | Board | `GET` | `/api/workspaces/{workspaceId}/board/init` | `getKanbanBoardInit` | Workspace doskasini yangi formatda olish | ✅ Faol |
| 19 | Chat | `GET` | `/api/chat/conversations` | `getDirectChatConversations` | DM suhbatlar ro'yxati (oxirgi xabar + unread count) | ✅ Faol |
| 20 | Chat | `POST` | `/api/chat/direct` | `sendDirectChatMessage` | Shaxsiy (DM) xabar yuborish | ✅ Faol |
| 21 | Chat | `GET` | `/api/chat/direct/{otherUserId}` | `getDirectChatMessages` | Foydalanuvchi bilan shaxsiy yozishmalar tarixini olish | ✅ Faol |
| 22 | Chat | `POST` | `/api/chat/direct/{otherUserId}/read-all` | `markAllDirectChatMessagesAsRead` | Suhbatdagi barcha xabarlarni o'qilgan deb belgilash | ✅ Faol |
| 23 | Chat | `GET` | `/api/chat/direct/{otherUserId}/search` | `searchDirectChatMessages` | DM suhbatdan xabar qidirish | ✅ Faol |
| 24 | Chat | `PUT` | `/api/chat/messages/{messageId}` | `editChatMessage` | Xabarni tahrirlash | ✅ Faol |
| 25 | Chat | `DELETE` | `/api/chat/messages/{messageId}` | `deleteChatMessage` | Xabarni o'chirish (hammadan) | ✅ Faol |
| 26 | Chat | `POST` | `/api/chat/messages/{messageId}/read` | `markChatMessageAsRead` | Xabarni o'qilgan deb belgilash | ✅ Faol |
| 27 | Chat | `GET` | `/api/chat/online` | `getOnlineChatUsers` | Hozir online foydalanuvchilar ro'yxati | ✅ Faol |
| 28 | Chat | `GET` | `/api/chat/public` | `getPublicChatMessages` | Umumiy chat xabarlarini sahifalab olish | ✅ Faol |
| 29 | Chat | `POST` | `/api/chat/public` | `sendPublicChatMessage` | Umumiy chatga xabar yuborish | ✅ Faol |
| 30 | Chat | `GET` | `/api/chat/public/search` | `searchPublicChatMessages` | Umumiy chatdan xabar qidirish | ✅ Faol |
| 31 | Chat | `GET` | `/api/chat/search` | `searchAllChatMessages` | Barcha ko'rinadigan xabarlar orasidan qidirish (Public + DM) | ✅ Faol |
| 32 | Chat | `GET` | `/api/chat/users` | `getChatUsers` | Chat uchun foydalanuvchilar ro'yxati (online holati bilan) | ✅ Faol |
| 33 | Chat | `POST` | `/api/chat/{messageId}/reactions` | `toggleChatMessageReaction` | Xabarga reaksiya qo'shish yoki olib tashlash (toggle) | ✅ Faol |
| 34 | Checklists | `GET` | `/api/tasks/{taskId}/checklists` | `getTaskChecklists` | Taskning barcha checklistlarini olish | ✅ Faol |
| 35 | Checklists | `POST` | `/api/tasks/{taskId}/checklists` | `createTaskChecklistItem` | Yangi checklist bandi qo'shish | ✅ Faol |
| 36 | Checklists | `PUT` | `/api/tasks/{taskId}/checklists/{itemId}` | `updateTaskChecklistItem` | Checklist bandini yangilash (sarlavha yoki bajarilganlik) | ✅ Faol |
| 37 | Checklists | `DELETE` | `/api/tasks/{taskId}/checklists/{itemId}` | `deleteTaskChecklistItem` | Checklist bandini o'chirish | ✅ Faol |
| 38 | Checklists | `GET` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists` | `getWorkspaceTaskChecklistsLegacy` | Taskning barcha checklistlarini olish (eski uslub) | ⚠️ Deprecated |
| 39 | Checklists | `POST` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists` | `createWorkspaceTaskChecklistItemLegacy` | Yangi checklist qo'shish (eski uslub) | ⚠️ Deprecated |
| 40 | Checklists | `PUT` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists/{itemId}` | `updateWorkspaceTaskChecklistItemLegacy` | Checklistni yangilash (eski uslub) | ⚠️ Deprecated |
| 41 | Checklists | `DELETE` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists/{itemId}` | `deleteWorkspaceTaskChecklistItemLegacy` | Checklistni o'chirish (eski uslub) | ⚠️ Deprecated |
| 42 | Columns | `GET` | `/api/workspaces/{workspaceId}/columns` | `getWorkspaceColumns` | Workspace ga tegishli ustunlar ro'yxatini olish | ✅ Faol |
| 43 | Columns | `POST` | `/api/workspaces/{workspaceId}/columns` | `createWorkspaceColumn` | Yangi ustun yaratish | ✅ Faol |
| 44 | Columns | `PATCH` | `/api/workspaces/{workspaceId}/columns` | `reorderWorkspaceColumnsLegacy` | Ustunlar tartibini o'zgartirish (eski variant) | ⚠️ Deprecated |
| 45 | Columns | `PATCH` | `/api/workspaces/{workspaceId}/columns/reorder` | `reorderWorkspaceColumns` | Ustunlar tartibini ID lar ro'yxati orqali yangilash (oddiy massiv: ['col1', 'col2']) | ✅ Faol |
| 46 | Columns | `PUT` | `/api/workspaces/{workspaceId}/columns/{id}` | `updateWorkspaceColumn` | Ustunni to'liq yangilash | ✅ Faol |
| 47 | Columns | `DELETE` | `/api/workspaces/{workspaceId}/columns/{id}` | `deleteWorkspaceColumn` | Ustunni o'chirish | ✅ Faol |
| 48 | Columns | `PATCH` | `/api/workspaces/{workspaceId}/columns/{id}` | `patchWorkspaceColumn` | Ustunni qisman yangilash | ✅ Faol |
| 49 | Comments | `GET` | `/api/tasks/{taskId}/comments` | `getTaskComments` | Task izohlari ro'yxatini sahifalab olish | ✅ Faol |
| 50 | Comments | `POST` | `/api/tasks/{taskId}/comments` | `createTaskComment` | Taskka yangi izoh qo'shish | ✅ Faol |
| 51 | Comments | `PUT` | `/api/tasks/{taskId}/comments/{commentId}` | `updateTaskComment` | Izohni tahrirlash | ✅ Faol |
| 52 | Comments | `DELETE` | `/api/tasks/{taskId}/comments/{commentId}` | `deleteTaskComment` | Izohni o'chirish | ✅ Faol |
| 53 | Config | `GET` | `/api/v1/config/public` | `getPublicSystemConfig` | Get public config | ✅ Faol |
| 54 | Directions | `GET` | `/api/workspaces/{workspaceId}/directions` | `getWorkspaceDirections` | Workspace ga tegishli barcha yo'nalishlarni olish | ✅ Faol |
| 55 | Directions | `POST` | `/api/workspaces/{workspaceId}/directions` | `createWorkspaceDirection` | Yangi yo'nalish qo'shish | ✅ Faol |
| 56 | Directions | `PUT` | `/api/workspaces/{workspaceId}/directions/{id}` | `updateWorkspaceDirection` | Yo'nalishni tahrirlash (nomi, rangi) | ✅ Faol |
| 57 | Directions | `DELETE` | `/api/workspaces/{workspaceId}/directions/{id}` | `deleteWorkspaceDirection` | Yo'nalishni o'chirish (Faqat OWNER/ADMIN) | ✅ Faol |
| 58 | Export | `GET` | `/api/workspaces/{workspaceId}/export/csv` | `exportWorkspaceTasksToCsv` | Workspace dagi barcha vazifalarni CSV formatida yuklash | ✅ Faol |
| 59 | Fayllar (Files) | `POST` | `/api/files/upload` | `uploadGeneralFile` | Fayl yuklash (xabar biriktirmasi uchun) | ✅ Faol |
| 60 | Fayllar (Files) | `GET` | `/api/files/{fileName}` | `downloadGeneralFile` | Faylni yuklab olish yoki ko'rish | ✅ Faol |
| 61 | Labels | `GET` | `/api/workspaces/{workspaceId}/labels` | `getWorkspaceLabels` | Workspace ga tegishli barcha labellarni olish | ✅ Faol |
| 62 | Labels | `POST` | `/api/workspaces/{workspaceId}/labels` | `createWorkspaceLabel` | Yangi label yaratish | ✅ Faol |
| 63 | Labels | `PUT` | `/api/workspaces/{workspaceId}/labels/{id}` | `updateWorkspaceLabel` | Label ni yangilash | ✅ Faol |
| 64 | Labels | `DELETE` | `/api/workspaces/{workspaceId}/labels/{id}` | `deleteWorkspaceLabel` | Label ni o'chirish | ✅ Faol |
| 65 | Notifications | `GET` | `/api/notifications` | `getMyNotifications` | Mening bildirishnomalarim ro'yxatini olish (Pageable) | ✅ Faol |
| 66 | Notifications | `PUT` | `/api/notifications/read-all` | `markAllNotificationsAsRead` | Barcha o'qilmaganlarni o'qilgan deb belgilash | ✅ Faol |
| 67 | Notifications | `PUT` | `/api/notifications/{id}/read` | `markNotificationAsRead` | Bildirishnomani o'qilgan deb belgilash | ✅ Faol |
| 68 | Reports | `GET` | `/api/workspaces/{workspaceId}/reports/sprints/{sprintId}` | `getSprintSummaryReport` | Muayyan Sprint bo'yicha ishlash tezligi (Burn-down) hisobotini olish | ✅ Faol |
| 69 | Reports | `GET` | `/api/workspaces/{workspaceId}/reports/summary` | `getWorkspaceSummaryReport` | Loyiha bo'yicha umumiy hisobot va xodimlar ish yukini olish | ✅ Faol |
| 70 | Sprints | `GET` | `/api/workspaces/{workspaceId}/backlog` | `getWorkspaceBacklog` | Workspace Backlog vazifalari ro'yxatini olish (sprintsiz tasklar) | ✅ Faol |
| 71 | Sprints | `GET` | `/api/workspaces/{workspaceId}/sprints` | `listWorkspaceSprints` | Workspacedagi sprintlar ro'yxatini olish (status bo'yicha filter qilish mumkin) | ✅ Faol |
| 72 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints` | `createWorkspaceSprint` | Yangi sprint yaratish | ✅ Faol |
| 73 | Sprints | `GET` | `/api/workspaces/{workspaceId}/sprints/{id}` | `getWorkspaceSprintById` | Sprint ma'lumotlarini id bo'yicha olish | ✅ Faol |
| 74 | Sprints | `PUT` | `/api/workspaces/{workspaceId}/sprints/{id}` | `updateWorkspaceSprint` | Sprintni tahrirlash | ✅ Faol |
| 75 | Sprints | `DELETE` | `/api/workspaces/{workspaceId}/sprints/{id}` | `deleteWorkspaceSprint` | Sprintni o'chirish (vazifalar backlogga qaytariladi) | ✅ Faol |
| 76 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints/{id}/complete` | `completeWorkspaceSprint` | Sprintni yakunlash (COMPLETED holatiga o'tkazish) | ✅ Faol |
| 77 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints/{id}/start` | `startWorkspaceSprint` | Sprintni boshlash (ACTIVE holatiga o'tkazish) | ✅ Faol |
| 78 | Sprints | `GET` | `/api/workspaces/{workspaceId}/sprints/{id}/tasks` | `getSprintTasks` | Sprintdagi barcha vazifalarni olish | ✅ Faol |
| 79 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints/{id}/tasks` | `addTasksToSprint` | Vazifalarni sprintga biriktirish | ✅ Faol |
| 80 | Sprints | `DELETE` | `/api/workspaces/{workspaceId}/sprints/{id}/tasks/{taskId}` | `removeTaskFromSprint` | Vazifani sprintdan chiqarib backlogga qaytarish | ✅ Faol |
| 81 | Task Activities | `GET` | `/api/tasks/{taskId}/activities` | `getTaskActivities` | Task o'zgarishlar tarixini sahifalab olish (History) | ✅ Faol |
| 82 | Tasks | `POST` | `/api/v2/workspaces/{workspaceId}/tasks` | `createTaskV2` | Yangi vazifa yaratish (v2) | ✅ Faol |
| 83 | Tasks | `GET` | `/api/workspaces/{workspaceId}/tasks` | `listWorkspaceTasks` | Workspace ga tegishli vazifalar ro'yxatini olish (qidiruv, filter va pagination bilan) | ✅ Faol |
| 84 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks` | `createWorkspaceTask` | Yangi vazifa yaratish | ✅ Faol |
| 85 | Tasks | `GET` | `/api/workspaces/{workspaceId}/tasks/{id}` | `getWorkspaceTaskById` | Bitta vazifani ID orqali olish (batafsil ma'lumotlari bilan) | ✅ Faol |
| 86 | Tasks | `PUT` | `/api/workspaces/{workspaceId}/tasks/{id}` | `updateWorkspaceTask` | Vazifani to'liq yangilash | ✅ Faol |
| 87 | Tasks | `DELETE` | `/api/workspaces/{workspaceId}/tasks/{id}` | `deleteWorkspaceTask` | Vazifani o'chirish | ✅ Faol |
| 88 | Tasks | `PATCH` | `/api/workspaces/{workspaceId}/tasks/{id}` | `patchWorkspaceTask` | Vazifani qisman yangilash | ✅ Faol |
| 89 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/archive` | `archiveWorkspaceTask` | Vazifani arxivga olish | ✅ Faol |
| 90 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/assign` | `toggleTaskAssignee` | Vazifaga foydalanuvchini biriktirish / olib tashlash | ✅ Faol |
| 91 | Tasks | `PATCH` | `/api/workspaces/{workspaceId}/tasks/{id}/reorder` | `reorderWorkspaceTask` | Vazifaning o'rnini (rank) va ustunini yangilash (Lexorank) | ✅ Faol |
| 92 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/unarchive` | `unarchiveWorkspaceTask` | Vazifani arxivdan chiqarish | ✅ Faol |
| 93 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/watch` | `toggleTaskWatch` | Vazifani kuzatish / kuzatishni to'xtatish (Watch) | ✅ Faol |
| 94 | Tasks Direct | `GET` | `/api/tasks/{id}` | `getTaskDirectById` | Vazifani bevosita ID orqali olish (eski/dublikat endpoint) | ⚠️ Deprecated |
| 95 | Tasks Direct | `GET` | `/api/tasks/{id}/details` | `getTaskDetails` | Vazifa to'liq ma'lumotlarini olish | ⚠️ Deprecated |
| 96 | Telegram Auth | `POST` | `/api/v1/auth/telegram` | `loginWithTelegram` | Telegram Mini App orqali login | ✅ Faol |
| 97 | Users | `GET` | `/api/auth/me` | `getCurrentUserLegacy` | Joriy foydalanuvchi profilini olish (eski alias) | ⚠️ Deprecated |
| 98 | Users | `GET` | `/api/me` | `getCurrentUser` | Joriy foydalanuvchi profilini olish | ✅ Faol |
| 99 | Users | `GET` | `/api/me/tasks` | `getMyAssignedTasksLegacy` | Joriy foydalanuvchiga biriktirilgan vazifalar (eski alias) | ⚠️ Deprecated |
| 100 | Users | `GET` | `/api/users` | `getWorkspaceUsers` | Workspace dagi foydalanuvchilar ro'yxatini olish | ✅ Faol |
| 101 | Users | `PATCH` | `/api/users/me` | `updateCurrentUserProfile` | Profil ma'lumotlarini tahrirlash | ✅ Faol |
| 102 | Users | `POST` | `/api/users/me/change-password` | `changePassword` | Parolni o'zgartirish | ✅ Faol |
| 103 | Users | `GET` | `/api/users/me/tasks` | `getMyAssignedTasks` | Joriy foydalanuvchiga biriktirilgan barcha vazifalar ro'yxatini olish (barcha workspacelar bo'yicha) | ✅ Faol |
| 104 | Users | `POST` | `/api/users/me/telegram` | `linkTelegram` | Mini App orqali Telegram akkauntni ulash | ✅ Faol |
| 105 | Users | `DELETE` | `/api/users/me/telegram` | `unlinkTelegram` | Telegram akkauntni uzish (faqat joriy foydalanuvchi uchun) | ✅ Faol |
| 106 | Users | `GET` | `/api/users/me/telegram-link-token` | `getTelegramLinkToken` | Telegram bilan ulash uchun vaqtinchalik token, tayyor havola va amal qilish muddatini olish | ✅ Faol |
| 107 | Workspace | `GET` | `/api/workspaces` | `getMyWorkspaces` | Foydalanuvchining workspace lari ro'yxatini olish | ✅ Faol |
| 108 | Workspace | `POST` | `/api/workspaces` | `createWorkspace` | Yangi workspace yaratish | ✅ Faol |
| 109 | Workspace | `GET` | `/api/workspaces/{id}` | `getWorkspaceById` | Bitta workspace ma'lumotlarini ID orqali olish | ✅ Faol |
| 110 | Workspace | `PUT` | `/api/workspaces/{id}` | `updateWorkspace` | Mavjud workspace ni tahrirlash | ✅ Faol |
| 111 | Workspace | `DELETE` | `/api/workspaces/{id}` | `deleteWorkspace` | Workspace ni o'chirish | ✅ Faol |
| 112 | Workspace Invitations | `POST` | `/api/invitations/by-id/{id}/accept` | `acceptWorkspaceInvitationById` | Taklifni ID bo'yicha qabul qilish (/me ro'yxatidan) | ✅ Faol |
| 113 | Workspace Invitations | `POST` | `/api/invitations/by-id/{id}/reject` | `rejectWorkspaceInvitationById` | Taklifni ID bo'yicha rad etish (/me ro'yxatidan) | ✅ Faol |
| 114 | Workspace Invitations | `GET` | `/api/invitations/me` | `getMyPendingInvitations` | Menga kelgan faol takliflarni ko'rish | ✅ Faol |
| 115 | Workspace Invitations | `PATCH` | `/api/invitations/{id}/accept` | `acceptWorkspaceInvitationLegacy` | Taklifni qabul qilish (Legacy PATCH) | ✅ Faol |
| 116 | Workspace Invitations | `GET` | `/api/invitations/{id}/details` | `getInvitationDetails` | Taklifnoma to'liq ma'lumotlarini olish (Ichki/Batafsil) | ✅ Faol |
| 117 | Workspace Invitations | `PATCH` | `/api/invitations/{id}/reject` | `rejectWorkspaceInvitationLegacy` | Taklifni rad etish (Legacy PATCH) | ✅ Faol |
| 118 | Workspace Invitations | `GET` | `/api/invitations/{token}` | `getPublicInvitationPreview` | Ommaviy taklifnoma ma'lumotlarini olish (Minimal/Xavfsiz) | ✅ Faol |
| 119 | Workspace Invitations | `POST` | `/api/invitations/{token}/accept` | `acceptWorkspaceInvitationByToken` | Taklifni qabul qilish | ✅ Faol |
| 120 | Workspace Invitations | `POST` | `/api/invitations/{token}/reject` | `rejectWorkspaceInvitationByToken` | Taklifni rad etish | ✅ Faol |
| 121 | Workspace Invitations | `GET` | `/api/workspaces/{workspaceId}/invites` | `getWorkspaceInvitations` | Workspace takliflarini ko'rish (Admin/Owner) | ✅ Faol |
| 122 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites` | `createWorkspaceEmailInvitation` | Email orqali taklif yuborish (Admin/Owner) | ✅ Faol |
| 123 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/link` | `createWorkspaceLinkInvitation` | Havola orqali taklif yaratish (Admin/Owner) | ✅ Faol |
| 124 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/link/regenerate` | `regenerateWorkspaceLinkInvitation` | Taklif havolasini yangilash (Admin/Owner) | ✅ Faol |
| 125 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/telegram-link` | `createTelegramInviteLink` | Telegram taklif havolasini yaratish (Admin/Owner) | ✅ Faol |
| 126 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/test-email` | `testBrevoEmailSending` | Brevo email yuborilishini tekshirish (Admin/Owner) | ✅ Faol |
| 127 | Workspace Invitations | `DELETE` | `/api/workspaces/{workspaceId}/invites/{id}` | `cancelWorkspaceInvitation` | Taklifni bekor qilish (Admin/Owner) | ✅ Faol |
| 128 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/{id}/resend` | `resendWorkspaceEmailInvitation` | Email taklifnomani qayta yuborish (Admin/Owner) | ✅ Faol |
| 129 | Workspace Members | `GET` | `/api/workspaces/{workspaceId}/members` | `getWorkspaceMembers` | Workspace a'zolari ro'yxatini rollari bilan olish | ✅ Faol |
| 130 | Workspace Members | `POST` | `/api/workspaces/{workspaceId}/members` | `inviteWorkspaceMember` | Workspace ga yangi a'zo taklif qilish / qo'shish | ✅ Faol |
| 131 | Workspace Members | `DELETE` | `/api/workspaces/{workspaceId}/members/{userId}` | `removeWorkspaceMember` | Workspace a'zosini chiqarib yuborish yoki jamoani tark etish | ✅ Faol |
| 132 | Workspace Members | `PATCH` | `/api/workspaces/{workspaceId}/members/{userId}` | `updateWorkspaceMemberRole` | Workspace a'zosining rolini o'zgartirish | ✅ Faol |
