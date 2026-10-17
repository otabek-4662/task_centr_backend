# OpenAPI Hujjati — Barcha Endpointlar va OperationId lar Ro'yxati

| # | Tag | HTTP Metod | Endpoint URL | OperationId | Tavsif | Holati |
|---|---|---|---|---|---|---|
| 1 | Admin Panel | `POST` | `/api/admin/cache/evict` | `evictAllCaches` | Barcha keshlarni tozalash | ✅ Faol |
| 2 | Admin Panel | `GET` | `/api/admin/dashboard` | `getDashboardStats` | Tizim ko'rsatkichlari (Dashboard) | ✅ Faol |
| 3 | Admin Panel | `GET` | `/api/admin/jobs` | `getSystemJobs` | Fon xizmatlari (Background Jobs) | ✅ Faol |
| 4 | Admin Panel | `POST` | `/api/admin/jobs/{jobKey}/trigger` | `triggerSystemJob` | Fon xizmatini qo'lda ishga tushirish | ✅ Faol |
| 5 | Admin Panel | `POST` | `/api/admin/telegram/broadcast` | `broadcastTelegramMessage` | Telegram orqali ommaviy xabarnoma (Broadcast) | ✅ Faol |
| 6 | Admin Panel | `GET` | `/api/admin/users` | `getUsers` | Foydalanuvchilar ro'yxati | ✅ Faol |
| 7 | Admin Panel | `DELETE` | `/api/admin/users/{userId}` | `deleteUser` | Foydalanuvchini o'chirish | ✅ Faol |
| 8 | Admin Panel | `PUT` | `/api/admin/users/{userId}/role` | `updateUserRole` | Foydalanuvchi rolini yangilash | ✅ Faol |
| 9 | Admin Panel | `GET` | `/api/admin/workspaces` | `getWorkspaces` | Ish maydonlari ro'yxati | ✅ Faol |
| 10 | Admin Panel | `DELETE` | `/api/admin/workspaces/{workspaceId}` | `deleteWorkspace_1` | Ish maydonini o'chirish | ✅ Faol |
| 11 | Attachments | `DELETE` | `/api/attachments/{attachmentId}` | `deleteTaskAttachment` | Biriktirilgan faylni o'chirish | ✅ Faol |
| 12 | Attachments | `GET` | `/api/attachments/{attachmentId}/download` | `downloadTaskAttachment` | Faylni yuklab olish yoki ko'rish | ✅ Faol |
| 13 | Attachments | `GET` | `/api/tasks/{taskId}/attachments` | `getTaskAttachments` | Taskka biriktirilgan barcha fayllar ro'yxatini olish | ✅ Faol |
| 14 | Attachments | `POST` | `/api/tasks/{taskId}/attachments` | `uploadTaskAttachment` | Taskka fayl yoki rasm yuklash | ✅ Faol |
| 15 | Auth | `POST` | `/api/auth/forgot-password` | `forgotPassword` | Parolni tiklash so'rovi (Email orqali havola yuborish) | ✅ Faol |
| 16 | Auth | `POST` | `/api/auth/login` | `loginUser` | Login - token beradi | ✅ Faol |
| 17 | Auth | `POST` | `/api/auth/logout` | `logoutUser` | Logout - sessiyani yakunlash | ✅ Faol |
| 18 | Auth | `POST` | `/api/auth/refresh` | `refreshToken` | Refresh - yangi token beradi | ✅ Faol |
| 19 | Auth | `POST` | `/api/auth/register` | `registerUser` | Register - token beradi | ✅ Faol |
| 20 | Auth | `POST` | `/api/auth/reset-password` | `resetPassword` | Yangi parol o'rnatish | ✅ Faol |
| 21 | Auth | `POST` | `/api/v1/auth/forgot-password` | `v1ForgotPassword` | Parolni tiklash so'rovi (eski v1 endpoint) | ⚠️ Deprecated |
| 22 | Auth | `POST` | `/api/v1/auth/login` | `v1LoginUser` | Login (eski v1 endpoint) | ⚠️ Deprecated |
| 23 | Auth | `POST` | `/api/v1/auth/logout` | `v1LogoutUser` | Logout (eski v1 endpoint) | ⚠️ Deprecated |
| 24 | Auth | `POST` | `/api/v1/auth/refresh` | `v1RefreshToken` | Refresh token (eski v1 endpoint) | ⚠️ Deprecated |
| 25 | Auth | `POST` | `/api/v1/auth/register` | `v1RegisterUser` | Register (eski v1 endpoint) | ⚠️ Deprecated |
| 26 | Auth | `POST` | `/api/v1/auth/reset-password` | `v1ResetPassword` | Yangi parol o'rnatish (eski v1 endpoint) | ⚠️ Deprecated |
| 27 | Board | `GET` | `/api/workspaces/{workspaceId}/board` | `getKanbanBoard` | Workspace doskasini barcha ustunlar va ularning vazifalari bilan olish | ✅ Faol |
| 28 | Board | `GET` | `/api/workspaces/{workspaceId}/board/init` | `getKanbanBoardInit` | Workspace doskasini yangi formatda olish | ✅ Faol |
| 29 | Calendar | `GET` | `/api/calendar/feed/{token}` | `getCalendarFeed` | Ommaviy iCal (.ics) kalendar oqimi (Google Calendar / Apple Calendar ulanishi) | ✅ Faol |
| 30 | Calendar | `GET` | `/api/calendar/feed/{token}.ics` | `getCalendarFeed_1` | Ommaviy iCal (.ics) kalendar oqimi (Google Calendar / Apple Calendar ulanishi) | ✅ Faol |
| 31 | Calendar | `GET` | `/api/users/me/calendar-link` | `getCalendarLink` | Foydalanuvchining shaxsiy iCal va Google Calendar havolasini olish | ✅ Faol |
| 32 | Chat | `GET` | `/api/chat/conversations` | `getDirectChatConversations` | DM suhbatlar ro'yxati (oxirgi xabar + unread count) | ✅ Faol |
| 33 | Chat | `POST` | `/api/chat/direct` | `sendDirectChatMessage` | Shaxsiy (DM) xabar yuborish | ✅ Faol |
| 34 | Chat | `GET` | `/api/chat/direct/{otherUserId}` | `getDirectChatMessages` | Foydalanuvchi bilan shaxsiy yozishmalar tarixini olish | ✅ Faol |
| 35 | Chat | `POST` | `/api/chat/direct/{otherUserId}/read-all` | `markAllDirectChatMessagesAsRead` | Suhbatdagi barcha xabarlarni o'qilgan deb belgilash | ✅ Faol |
| 36 | Chat | `GET` | `/api/chat/direct/{otherUserId}/search` | `searchDirectChatMessages` | DM suhbatdan xabar qidirish | ✅ Faol |
| 37 | Chat | `PUT` | `/api/chat/messages/{messageId}` | `editChatMessage` | Xabarni tahrirlash | ✅ Faol |
| 38 | Chat | `DELETE` | `/api/chat/messages/{messageId}` | `deleteChatMessage` | Xabarni o'chirish (hammadan) | ✅ Faol |
| 39 | Chat | `POST` | `/api/chat/messages/{messageId}/read` | `markChatMessageAsRead` | Xabarni o'qilgan deb belgilash | ✅ Faol |
| 40 | Chat | `GET` | `/api/chat/online` | `getOnlineChatUsers` | Hozir online foydalanuvchilar ro'yxati | ✅ Faol |
| 41 | Chat | `GET` | `/api/chat/public` | `getPublicChatMessages` | Umumiy chat xabarlarini sahifalab olish | ✅ Faol |
| 42 | Chat | `POST` | `/api/chat/public` | `sendPublicChatMessage` | Umumiy chatga xabar yuborish | ✅ Faol |
| 43 | Chat | `GET` | `/api/chat/public/search` | `searchPublicChatMessages` | Umumiy chatdan xabar qidirish | ✅ Faol |
| 44 | Chat | `GET` | `/api/chat/search` | `searchAllChatMessages` | Barcha ko'rinadigan xabarlar orasidan qidirish (Public + DM) | ✅ Faol |
| 45 | Chat | `GET` | `/api/chat/users` | `getChatUsers` | Chat uchun foydalanuvchilar ro'yxati (online holati bilan) | ✅ Faol |
| 46 | Chat | `POST` | `/api/chat/{messageId}/reactions` | `toggleChatMessageReaction` | Xabarga reaksiya qo'shish yoki olib tashlash (toggle) | ✅ Faol |
| 47 | Checklists | `GET` | `/api/tasks/{taskId}/checklists` | `getTaskChecklists` | Taskning barcha checklistlarini olish | ✅ Faol |
| 48 | Checklists | `POST` | `/api/tasks/{taskId}/checklists` | `createTaskChecklistItem` | Yangi checklist bandi qo'shish | ✅ Faol |
| 49 | Checklists | `PUT` | `/api/tasks/{taskId}/checklists/{itemId}` | `updateTaskChecklistItem` | Checklist bandini yangilash (sarlavha yoki bajarilganlik) | ✅ Faol |
| 50 | Checklists | `DELETE` | `/api/tasks/{taskId}/checklists/{itemId}` | `deleteTaskChecklistItem` | Checklist bandini o'chirish | ✅ Faol |
| 51 | Checklists | `GET` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists` | `getWorkspaceTaskChecklistsLegacy` | Taskning barcha checklistlarini olish (eski uslub) | ⚠️ Deprecated |
| 52 | Checklists | `POST` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists` | `createWorkspaceTaskChecklistItemLegacy` | Yangi checklist qo'shish (eski uslub) | ⚠️ Deprecated |
| 53 | Checklists | `PUT` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists/{itemId}` | `updateWorkspaceTaskChecklistItemLegacy` | Checklistni yangilash (eski uslub) | ⚠️ Deprecated |
| 54 | Checklists | `DELETE` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists/{itemId}` | `deleteWorkspaceTaskChecklistItemLegacy` | Checklistni o'chirish (eski uslub) | ⚠️ Deprecated |
| 55 | Columns | `GET` | `/api/workspaces/{workspaceId}/columns` | `getWorkspaceColumns` | Workspace ga tegishli ustunlar ro'yxatini olish | ✅ Faol |
| 56 | Columns | `POST` | `/api/workspaces/{workspaceId}/columns` | `createWorkspaceColumn` | Yangi ustun yaratish | ✅ Faol |
| 57 | Columns | `PATCH` | `/api/workspaces/{workspaceId}/columns` | `reorderWorkspaceColumnsLegacy` | Ustunlar tartibini o'zgartirish (eski variant) | ⚠️ Deprecated |
| 58 | Columns | `PATCH` | `/api/workspaces/{workspaceId}/columns/reorder` | `reorderWorkspaceColumns` | Ustunlar tartibini ID lar ro'yxati orqali yangilash (oddiy massiv: ['col1', 'col2']) | ✅ Faol |
| 59 | Columns | `PUT` | `/api/workspaces/{workspaceId}/columns/{id}` | `updateWorkspaceColumn` | Ustunni to'liq yangilash | ✅ Faol |
| 60 | Columns | `DELETE` | `/api/workspaces/{workspaceId}/columns/{id}` | `deleteWorkspaceColumn` | Ustunni o'chirish | ✅ Faol |
| 61 | Columns | `PATCH` | `/api/workspaces/{workspaceId}/columns/{id}` | `patchWorkspaceColumn` | Ustunni qisman yangilash | ✅ Faol |
| 62 | Comments | `GET` | `/api/tasks/{taskId}/comments` | `getTaskComments` | Task izohlari ro'yxatini sahifalab olish | ✅ Faol |
| 63 | Comments | `POST` | `/api/tasks/{taskId}/comments` | `createTaskComment` | Taskka yangi izoh qo'shish | ✅ Faol |
| 64 | Comments | `PUT` | `/api/tasks/{taskId}/comments/{commentId}` | `updateTaskComment` | Izohni tahrirlash | ✅ Faol |
| 65 | Comments | `DELETE` | `/api/tasks/{taskId}/comments/{commentId}` | `deleteTaskComment` | Izohni o'chirish | ✅ Faol |
| 66 | Config | `GET` | `/api/v1/config/public` | `getPublicSystemConfig` | Get public config | ✅ Faol |
| 67 | Contacts | `GET` | `/api/contacts` | `getMyContacts` | Foydalanuvchining shaxsiy kontaktlari ro'yxatini olish | ✅ Faol |
| 68 | Contacts | `POST` | `/api/contacts` | `addContact` | Login nomi yoki email orqali yangi kontakt qo'shish | ✅ Faol |
| 69 | Contacts | `DELETE` | `/api/contacts/{contactUserId}` | `deleteContact` | Kontaktni ro'yxatdan o'chirish | ✅ Faol |
| 70 | Directions | `GET` | `/api/workspaces/{workspaceId}/directions` | `getWorkspaceDirections` | Workspace ga tegishli barcha yo'nalishlarni olish | ✅ Faol |
| 71 | Directions | `POST` | `/api/workspaces/{workspaceId}/directions` | `createWorkspaceDirection` | Yangi yo'nalish qo'shish | ✅ Faol |
| 72 | Directions | `PUT` | `/api/workspaces/{workspaceId}/directions/{id}` | `updateWorkspaceDirection` | Yo'nalishni tahrirlash (nomi, rangi) | ✅ Faol |
| 73 | Directions | `DELETE` | `/api/workspaces/{workspaceId}/directions/{id}` | `deleteWorkspaceDirection` | Yo'nalishni o'chirish (Faqat OWNER/ADMIN) | ✅ Faol |
| 74 | Export | `GET` | `/api/workspaces/{workspaceId}/export/csv` | `exportWorkspaceTasksToCsv` | Workspace dagi barcha vazifalarni CSV formatida yuklash | ✅ Faol |
| 75 | Export | `GET` | `/api/workspaces/{workspaceId}/export/daily-report.pdf` | `exportWorkspaceDailyReportPdf` | Workspace ning kunlik hisobotini PDF formatida yuklash | ✅ Faol |
| 76 | Fayllar (Files) | `GET` | `/api/files/chat-attachments/{fileName}` | `downloadChatAttachmentFile` | Chat biriktirmasini yuklab olish yoki ko'rish | ✅ Faol |
| 77 | Fayllar (Files) | `POST` | `/api/files/upload` | `uploadGeneralFile` | Fayl yuklash (xabar biriktirmasi uchun) | ✅ Faol |
| 78 | Fayllar (Files) | `GET` | `/api/files/{fileName}` | `downloadGeneralFile` | Faylni yuklab olish yoki ko'rish | ✅ Faol |
| 79 | Focus Timers | `GET` | `/api/timers/active` | `getActiveTimer` | Joriy foydalanuvchining faol taymerini olish | ✅ Faol |
| 80 | Focus Timers | `POST` | `/api/timers/start` | `startTimer` | Pomodoro taymerini boshlash | ✅ Faol |
| 81 | Focus Timers | `POST` | `/api/timers/stop` | `stopTimer` | Faol Pomodoro taymerini to'xtatish va ishlangan vaqtni yozish | ✅ Faol |
| 82 | Labels | `GET` | `/api/workspaces/{workspaceId}/labels` | `getWorkspaceLabels` | Workspace ga tegishli barcha labellarni olish | ✅ Faol |
| 83 | Labels | `POST` | `/api/workspaces/{workspaceId}/labels` | `createWorkspaceLabel` | Yangi label yaratish | ✅ Faol |
| 84 | Labels | `PUT` | `/api/workspaces/{workspaceId}/labels/{id}` | `updateWorkspaceLabel` | Label ni yangilash | ✅ Faol |
| 85 | Labels | `DELETE` | `/api/workspaces/{workspaceId}/labels/{id}` | `deleteWorkspaceLabel` | Label ni o'chirish | ✅ Faol |
| 86 | Notifications | `GET` | `/api/notifications` | `getMyNotifications` | Mening bildirishnomalarim ro'yxatini olish (Pageable) | ✅ Faol |
| 87 | Notifications | `PUT` | `/api/notifications/read-all` | `markAllNotificationsAsRead` | Barcha o'qilmaganlarni o'qilgan deb belgilash | ✅ Faol |
| 88 | Notifications | `PUT` | `/api/notifications/{id}/read` | `markNotificationAsRead` | Bildirishnomani o'qilgan deb belgilash | ✅ Faol |
| 89 | Reports | `GET` | `/api/workspaces/{workspaceId}/reports/sprints/{sprintId}` | `getSprintSummaryReport` | Muayyan Sprint bo'yicha ishlash tezligi (Burn-down) hisobotini olish | ✅ Faol |
| 90 | Reports | `GET` | `/api/workspaces/{workspaceId}/reports/summary` | `getWorkspaceSummaryReport` | Loyiha bo'yicha umumiy hisobot va xodimlar ish yukini olish | ✅ Faol |
| 91 | Sprints | `GET` | `/api/workspaces/{workspaceId}/backlog` | `getWorkspaceBacklog` | Workspace Backlog vazifalari ro'yxatini olish (sprintsiz tasklar) | ✅ Faol |
| 92 | Sprints | `GET` | `/api/workspaces/{workspaceId}/sprints` | `listWorkspaceSprints` | Workspacedagi sprintlar ro'yxatini olish (status bo'yicha filter qilish mumkin) | ✅ Faol |
| 93 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints` | `createWorkspaceSprint` | Yangi sprint yaratish | ✅ Faol |
| 94 | Sprints | `GET` | `/api/workspaces/{workspaceId}/sprints/{id}` | `getWorkspaceSprintById` | Sprint ma'lumotlarini id bo'yicha olish | ✅ Faol |
| 95 | Sprints | `PUT` | `/api/workspaces/{workspaceId}/sprints/{id}` | `updateWorkspaceSprint` | Sprintni tahrirlash | ✅ Faol |
| 96 | Sprints | `DELETE` | `/api/workspaces/{workspaceId}/sprints/{id}` | `deleteWorkspaceSprint` | Sprintni o'chirish (vazifalar backlogga qaytariladi) | ✅ Faol |
| 97 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints/{id}/complete` | `completeWorkspaceSprint` | Sprintni yakunlash (COMPLETED holatiga o'tkazish) | ✅ Faol |
| 98 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints/{id}/start` | `startWorkspaceSprint` | Sprintni boshlash (ACTIVE holatiga o'tkazish) | ✅ Faol |
| 99 | Sprints | `GET` | `/api/workspaces/{workspaceId}/sprints/{id}/tasks` | `getSprintTasks` | Sprintdagi barcha vazifalarni olish | ✅ Faol |
| 100 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints/{id}/tasks` | `addTasksToSprint` | Vazifalarni sprintga biriktirish | ✅ Faol |
| 101 | Sprints | `DELETE` | `/api/workspaces/{workspaceId}/sprints/{id}/tasks/{taskId}` | `removeTaskFromSprint` | Vazifani sprintdan chiqarib backlogga qaytarish | ✅ Faol |
| 102 | Task Activities | `GET` | `/api/tasks/{taskId}/activities` | `getTaskActivities` | Task o'zgarishlar tarixini sahifalab olish (History) | ✅ Faol |
| 103 | Task Activities | `GET` | `/api/workspaces/{workspaceId}/activities` | `getWorkspaceActivities` | Workspace bo'yicha barcha vazifalar o'zgarishlar tarixini (Activity Feed) sahifalab olish | ✅ Faol |
| 104 | Task Dependencies | `GET` | `/api/tasks/{taskId}/dependencies` | `getDependencies` | Vazifaning bog'liqliklarini olish | ✅ Faol |
| 105 | Task Dependencies | `POST` | `/api/tasks/{taskId}/dependencies` | `addDependency` | Vazifaga yangi to'siq (Blocker) bog'liqlik qo'shish | ✅ Faol |
| 106 | Task Dependencies | `DELETE` | `/api/tasks/{taskId}/dependencies/{dependencyId}` | `removeDependency` | Bog'liqlikni o'chirish | ✅ Faol |
| 107 | Tasks | `POST` | `/api/v2/workspaces/{workspaceId}/tasks` | `createTaskV2` | Yangi vazifa yaratish (v2) | ✅ Faol |
| 108 | Tasks | `GET` | `/api/workspaces/{workspaceId}/tasks` | `listWorkspaceTasks` | Workspace ga tegishli vazifalar ro'yxatini olish (qidiruv, filter va pagination bilan) | ✅ Faol |
| 109 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks` | `createWorkspaceTask` | Yangi vazifa yaratish | ✅ Faol |
| 110 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/columns/{columnId}/rebalance` | `rebalanceColumnLexoRanks` | Ustundagi barcha vazifalar LexoRank tartibini qayta muvozanatlash (Rebalance) | ✅ Faol |
| 111 | Tasks | `GET` | `/api/workspaces/{workspaceId}/tasks/{id}` | `getWorkspaceTaskById` | Bitta vazifani ID orqali olish (batafsil ma'lumotlari bilan) | ✅ Faol |
| 112 | Tasks | `PUT` | `/api/workspaces/{workspaceId}/tasks/{id}` | `updateWorkspaceTask` | Vazifani to'liq yangilash | ✅ Faol |
| 113 | Tasks | `DELETE` | `/api/workspaces/{workspaceId}/tasks/{id}` | `deleteWorkspaceTask` | Vazifani o'chirish | ✅ Faol |
| 114 | Tasks | `PATCH` | `/api/workspaces/{workspaceId}/tasks/{id}` | `patchWorkspaceTask` | Vazifani qisman yangilash | ✅ Faol |
| 115 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/archive` | `archiveWorkspaceTask` | Vazifani arxivga olish | ✅ Faol |
| 116 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/assign` | `toggleTaskAssignee` | Vazifaga foydalanuvchini biriktirish / olib tashlash | ✅ Faol |
| 117 | Tasks | `PATCH` | `/api/workspaces/{workspaceId}/tasks/{id}/reorder` | `reorderWorkspaceTask` | Vazifaning o'rnini (rank) va ustunini yangilash (Lexorank) | ✅ Faol |
| 118 | Tasks | `GET` | `/api/workspaces/{workspaceId}/tasks/{id}/subtasks` | `getTaskSubtasks` | Ota vazifaga biriktirilgan ostki vazifalar (Subtasks) ro'yxatini olish | ✅ Faol |
| 119 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/unarchive` | `unarchiveWorkspaceTask` | Vazifani arxivdan chiqarish | ✅ Faol |
| 120 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/watch` | `toggleTaskWatch` | Vazifani kuzatish / kuzatishni to'xtatish (Watch) | ✅ Faol |
| 121 | Tasks | `GET` | `/api/workspaces/{workspaceId}/tasks/{id}/worklogs` | `getTaskWorklogs` | Vazifa bo'yicha sarflangan ish vaqti (Worklogs) tarixini olish | ✅ Faol |
| 122 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/worklogs` | `addTaskWorklog` | Vazifaga sarflangan ish vaqtini (Worklog) qo'shish | ✅ Faol |
| 123 | Tasks Direct | `GET` | `/api/tasks/{id}` | `getTaskDirectById` | Vazifani bevosita ID orqali olish (eski/dublikat endpoint) | ⚠️ Deprecated |
| 124 | Tasks Direct | `GET` | `/api/tasks/{id}/details` | `getTaskDetails` | Vazifa to'liq ma'lumotlarini olish | ⚠️ Deprecated |
| 125 | Telegram Auth | `POST` | `/api/auth/telegram` | `loginWithTelegram` | Telegram Mini App orqali login (Tavsiya etilgan usul) | ✅ Faol |
| 126 | Telegram Auth | `POST` | `/api/auth/telegram/pending-invite` | `createPendingTelegramInvite_1` | Mini App ulanmagan foydalanuvchi uchun kutilayotgan taklif yaratish | ✅ Faol |
| 127 | Telegram Auth | `POST` | `/api/v1/auth/telegram` | `loginWithTelegram_1` | Telegram Mini App orqali login (Tavsiya etilgan usul) | ✅ Faol |
| 128 | Telegram Auth | `POST` | `/api/v1/auth/telegram/pending-invite` | `createPendingTelegramInvite` | Mini App ulanmagan foydalanuvchi uchun kutilayotgan taklif yaratish | ✅ Faol |
| 129 | Users | `GET` | `/api/auth/me` | `getCurrentUserLegacy` | Joriy foydalanuvchi profilini olish (eski alias) | ⚠️ Deprecated |
| 130 | Users | `GET` | `/api/me` | `getCurrentUser` | Joriy foydalanuvchi profilini olish | ✅ Faol |
| 131 | Users | `GET` | `/api/me/tasks` | `getMyAssignedTasksLegacy` | Joriy foydalanuvchiga biriktirilgan vazifalar (eski alias) | ⚠️ Deprecated |
| 132 | Users | `GET` | `/api/users` | `getWorkspaceUsers` | Workspace dagi foydalanuvchilar ro'yxatini olish | ✅ Faol |
| 133 | Users | `PATCH` | `/api/users/me` | `updateCurrentUserProfile` | Profil ma'lumotlarini tahrirlash | ✅ Faol |
| 134 | Users | `POST` | `/api/users/me/change-password` | `changePassword` | Parolni o'zgartirish | ✅ Faol |
| 135 | Users | `GET` | `/api/users/me/tasks` | `getMyAssignedTasks` | Joriy foydalanuvchiga biriktirilgan barcha vazifalar ro'yxatini olish (barcha workspacelar bo'yicha) | ✅ Faol |
| 136 | Users | `POST` | `/api/users/me/telegram` | `linkTelegram` | Mini App orqali Telegram akkauntni ulash | ✅ Faol |
| 137 | Users | `DELETE` | `/api/users/me/telegram` | `unlinkTelegram` | Telegram akkauntni uzish (faqat joriy foydalanuvchi uchun) | ✅ Faol |
| 138 | Users | `GET` | `/api/users/me/telegram-link-token` | `getTelegramLinkToken` | Telegram bilan ulash uchun vaqtinchalik token, tayyor havola va amal qilish muddatini olish | ✅ Faol |
| 139 | Workspace | `GET` | `/api/workspaces` | `getMyWorkspaces` | Foydalanuvchining workspace lari ro'yxatini olish | ✅ Faol |
| 140 | Workspace | `POST` | `/api/workspaces` | `createWorkspace` | Yangi workspace yaratish | ✅ Faol |
| 141 | Workspace | `GET` | `/api/workspaces/{id}` | `getWorkspaceById` | Bitta workspace ma'lumotlarini ID orqali olish | ✅ Faol |
| 142 | Workspace | `PUT` | `/api/workspaces/{id}` | `updateWorkspace` | Mavjud workspace ni tahrirlash | ✅ Faol |
| 143 | Workspace | `DELETE` | `/api/workspaces/{id}` | `deleteWorkspace` | Workspace ni o'chirish | ✅ Faol |
| 144 | Workspace Invitations | `POST` | `/api/invitations/by-id/{id}/accept` | `acceptWorkspaceInvitationById` | Taklifni ID bo'yicha qabul qilish (/me ro'yxatidan) | ✅ Faol |
| 145 | Workspace Invitations | `POST` | `/api/invitations/by-id/{id}/reject` | `rejectWorkspaceInvitationById` | Taklifni ID bo'yicha rad etish (/me ro'yxatidan) | ✅ Faol |
| 146 | Workspace Invitations | `GET` | `/api/invitations/me` | `getMyPendingInvitations` | Menga kelgan faol takliflarni ko'rish | ✅ Faol |
| 147 | Workspace Invitations | `PATCH` | `/api/invitations/{id}/accept` | `acceptWorkspaceInvitationLegacy` | Taklifni qabul qilish (Legacy PATCH) | ✅ Faol |
| 148 | Workspace Invitations | `GET` | `/api/invitations/{id}/details` | `getInvitationDetails` | Taklifnoma to'liq ma'lumotlarini olish (Ichki/Batafsil) | ✅ Faol |
| 149 | Workspace Invitations | `PATCH` | `/api/invitations/{id}/reject` | `rejectWorkspaceInvitationLegacy` | Taklifni rad etish (Legacy PATCH) | ✅ Faol |
| 150 | Workspace Invitations | `GET` | `/api/invitations/{token}` | `getPublicInvitationPreview` | Ommaviy taklifnoma ma'lumotlarini olish (Minimal/Xavfsiz) | ✅ Faol |
| 151 | Workspace Invitations | `POST` | `/api/invitations/{token}/accept` | `acceptWorkspaceInvitationByToken` | Taklifni qabul qilish | ✅ Faol |
| 152 | Workspace Invitations | `POST` | `/api/invitations/{token}/reject` | `rejectWorkspaceInvitationByToken` | Taklifni rad etish | ✅ Faol |
| 153 | Workspace Invitations | `GET` | `/api/workspaces/{workspaceId}/invites` | `getWorkspaceInvitations` | Workspace takliflarini ko'rish (Admin/Owner) | ✅ Faol |
| 154 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites` | `createWorkspaceEmailInvitation` | Email orqali taklif yuborish (Admin/Owner) | ✅ Faol |
| 155 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/link` | `createWorkspaceLinkInvitation` | Havola orqali taklif yaratish (Admin/Owner) | ✅ Faol |
| 156 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/link/regenerate` | `regenerateWorkspaceLinkInvitation` | Taklif havolasini yangilash (Admin/Owner) | ✅ Faol |
| 157 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/telegram-link` | `createTelegramInviteLink` | Telegram taklif havolasini yaratish (Admin/Owner) | ✅ Faol |
| 158 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/test-email` | `testBrevoEmailSending` | Brevo email yuborilishini tekshirish (Admin/Owner) | ✅ Faol |
| 159 | Workspace Invitations | `DELETE` | `/api/workspaces/{workspaceId}/invites/{id}` | `cancelWorkspaceInvitation` | Taklifni bekor qilish (Admin/Owner) | ✅ Faol |
| 160 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/{id}/resend` | `resendWorkspaceEmailInvitation` | Email taklifnomani qayta yuborish (Admin/Owner) | ✅ Faol |
| 161 | Workspace Members | `GET` | `/api/workspaces/{workspaceId}/members` | `getWorkspaceMembers` | Workspace a'zolari ro'yxatini rollari bilan olish | ✅ Faol |
| 162 | Workspace Members | `POST` | `/api/workspaces/{workspaceId}/members` | `inviteWorkspaceMember` | Workspace ga yangi a'zo taklif qilish / qo'shish | ✅ Faol |
| 163 | Workspace Members | `DELETE` | `/api/workspaces/{workspaceId}/members/{userId}` | `removeWorkspaceMember` | Workspace a'zosini chiqarib yuborish yoki jamoani tark etish | ✅ Faol |
| 164 | Workspace Members | `PATCH` | `/api/workspaces/{workspaceId}/members/{userId}` | `updateWorkspaceMemberRole` | Workspace a'zosining rolini o'zgartirish | ✅ Faol |
