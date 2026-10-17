# OpenAPI Hujjati — Barcha Endpointlar va OperationId lar Ro'yxati

| # | Tag | HTTP Metod | Endpoint URL | OperationId | Tavsif | Holati |
|---|---|---|---|---|---|---|
| 1 | Admin Panel | `GET` | `/api/admin/audit` | `getAuditLogs` | Platforma audit jurnali | ✅ Faol |
| 2 | Admin Panel | `POST` | `/api/admin/cache/evict` | `evictAllCaches` | Barcha keshlarni tozalash | ✅ Faol |
| 3 | Admin Panel | `GET` | `/api/admin/dashboard` | `getDashboardStats` | Tizim ko'rsatkichlari (Dashboard) | ✅ Faol |
| 4 | Admin Panel | `GET` | `/api/admin/jobs` | `getSystemJobs` | Fon xizmatlari (Background Jobs) | ✅ Faol |
| 5 | Admin Panel | `POST` | `/api/admin/jobs/{jobKey}/trigger` | `triggerSystemJob` | Fon xizmatini qo'lda ishga tushirish | ✅ Faol |
| 6 | Admin Panel | `POST` | `/api/admin/telegram/broadcast` | `broadcastTelegramMessage` | Telegram orqali ommaviy xabarnoma (Broadcast) | ✅ Faol |
| 7 | Admin Panel | `GET` | `/api/admin/users` | `getUsers` | Foydalanuvchilar ro'yxati | ✅ Faol |
| 8 | Admin Panel | `DELETE` | `/api/admin/users/{userId}` | `deleteUser` | Foydalanuvchini o'chirish | ✅ Faol |
| 9 | Admin Panel | `PUT` | `/api/admin/users/{userId}/role` | `updateUserRole` | Foydalanuvchi rolini yangilash | ✅ Faol |
| 10 | Admin Panel | `GET` | `/api/admin/workspaces` | `getWorkspaces` | Ish maydonlari ro'yxati | ✅ Faol |
| 11 | Admin Panel | `DELETE` | `/api/admin/workspaces/{workspaceId}` | `deleteWorkspace_1` | Ish maydonini o'chirish | ✅ Faol |
| 12 | Attachments | `DELETE` | `/api/attachments/{attachmentId}` | `deleteTaskAttachment` | Biriktirilgan faylni o'chirish | ✅ Faol |
| 13 | Attachments | `GET` | `/api/attachments/{attachmentId}/download` | `downloadTaskAttachment` | Faylni yuklab olish yoki ko'rish | ✅ Faol |
| 14 | Attachments | `GET` | `/api/tasks/{taskId}/attachments` | `getTaskAttachments` | Taskka biriktirilgan barcha fayllar ro'yxatini olish | ✅ Faol |
| 15 | Attachments | `POST` | `/api/tasks/{taskId}/attachments` | `uploadTaskAttachment` | Taskka fayl yoki rasm yuklash | ✅ Faol |
| 16 | Auth | `POST` | `/api/auth/forgot-password` | `forgotPassword` | Parolni tiklash so'rovi (Email orqali havola yuborish) | ✅ Faol |
| 17 | Auth | `POST` | `/api/auth/login` | `loginUser` | Login - token beradi | ✅ Faol |
| 18 | Auth | `POST` | `/api/auth/logout` | `logoutUser` | Logout - sessiyani yakunlash | ✅ Faol |
| 19 | Auth | `POST` | `/api/auth/refresh` | `refreshToken` | Refresh - yangi token beradi | ✅ Faol |
| 20 | Auth | `POST` | `/api/auth/register` | `registerUser` | Register - token beradi | ✅ Faol |
| 21 | Auth | `POST` | `/api/auth/reset-password` | `resetPassword` | Yangi parol o'rnatish | ✅ Faol |
| 22 | Auth | `POST` | `/api/v1/auth/forgot-password` | `v1ForgotPassword` | Parolni tiklash so'rovi (eski v1 endpoint) | ⚠️ Deprecated |
| 23 | Auth | `POST` | `/api/v1/auth/login` | `v1LoginUser` | Login (eski v1 endpoint) | ⚠️ Deprecated |
| 24 | Auth | `POST` | `/api/v1/auth/logout` | `v1LogoutUser` | Logout (eski v1 endpoint) | ⚠️ Deprecated |
| 25 | Auth | `POST` | `/api/v1/auth/refresh` | `v1RefreshToken` | Refresh token (eski v1 endpoint) | ⚠️ Deprecated |
| 26 | Auth | `POST` | `/api/v1/auth/register` | `v1RegisterUser` | Register (eski v1 endpoint) | ⚠️ Deprecated |
| 27 | Auth | `POST` | `/api/v1/auth/reset-password` | `v1ResetPassword` | Yangi parol o'rnatish (eski v1 endpoint) | ⚠️ Deprecated |
| 28 | Board | `GET` | `/api/workspaces/{workspaceId}/board` | `getKanbanBoard` | Workspace doskasini barcha ustunlar va ularning vazifalari bilan olish | ✅ Faol |
| 29 | Board | `GET` | `/api/workspaces/{workspaceId}/board/init` | `getKanbanBoardInit` | Workspace doskasini yangi formatda olish | ✅ Faol |
| 30 | Calendar | `GET` | `/api/calendar/feed/{token}` | `getCalendarFeed` | Ommaviy iCal (.ics) kalendar oqimi (Google Calendar / Apple Calendar ulanishi) | ✅ Faol |
| 31 | Calendar | `GET` | `/api/calendar/feed/{token}.ics` | `getCalendarFeed_1` | Ommaviy iCal (.ics) kalendar oqimi (Google Calendar / Apple Calendar ulanishi) | ✅ Faol |
| 32 | Calendar | `GET` | `/api/users/me/calendar-link` | `getCalendarLink` | Foydalanuvchining shaxsiy iCal va Google Calendar havolasini olish | ✅ Faol |
| 33 | Chat | `GET` | `/api/chat/conversations` | `getDirectChatConversations` | DM suhbatlar ro'yxati (oxirgi xabar + unread count) | ✅ Faol |
| 34 | Chat | `POST` | `/api/chat/direct` | `sendDirectChatMessage` | Shaxsiy (DM) xabar yuborish | ✅ Faol |
| 35 | Chat | `GET` | `/api/chat/direct/{otherUserId}` | `getDirectChatMessages` | Foydalanuvchi bilan shaxsiy yozishmalar tarixini olish | ✅ Faol |
| 36 | Chat | `POST` | `/api/chat/direct/{otherUserId}/read-all` | `markAllDirectChatMessagesAsRead` | Suhbatdagi barcha xabarlarni o'qilgan deb belgilash | ✅ Faol |
| 37 | Chat | `GET` | `/api/chat/direct/{otherUserId}/search` | `searchDirectChatMessages` | DM suhbatdan xabar qidirish | ✅ Faol |
| 38 | Chat | `PUT` | `/api/chat/messages/{messageId}` | `editChatMessage` | Xabarni tahrirlash | ✅ Faol |
| 39 | Chat | `DELETE` | `/api/chat/messages/{messageId}` | `deleteChatMessage` | Xabarni o'chirish (hammadan) | ✅ Faol |
| 40 | Chat | `POST` | `/api/chat/messages/{messageId}/read` | `markChatMessageAsRead` | Xabarni o'qilgan deb belgilash | ✅ Faol |
| 41 | Chat | `GET` | `/api/chat/online` | `getOnlineChatUsers` | Hozir online foydalanuvchilar ro'yxati | ✅ Faol |
| 42 | Chat | `GET` | `/api/chat/public` | `getPublicChatMessages` | Umumiy chat xabarlarini sahifalab olish | ✅ Faol |
| 43 | Chat | `POST` | `/api/chat/public` | `sendPublicChatMessage` | Umumiy chatga xabar yuborish | ✅ Faol |
| 44 | Chat | `GET` | `/api/chat/public/search` | `searchPublicChatMessages` | Umumiy chatdan xabar qidirish | ✅ Faol |
| 45 | Chat | `GET` | `/api/chat/search` | `searchAllChatMessages` | Barcha ko'rinadigan xabarlar orasidan qidirish (Public + DM) | ✅ Faol |
| 46 | Chat | `GET` | `/api/chat/users` | `getChatUsers` | Chat uchun foydalanuvchilar ro'yxati (online holati bilan) | ✅ Faol |
| 47 | Chat | `POST` | `/api/chat/{messageId}/reactions` | `toggleChatMessageReaction` | Xabarga reaksiya qo'shish yoki olib tashlash (toggle) | ✅ Faol |
| 48 | Checklists | `GET` | `/api/tasks/{taskId}/checklists` | `getTaskChecklists` | Taskning barcha checklistlarini olish | ✅ Faol |
| 49 | Checklists | `POST` | `/api/tasks/{taskId}/checklists` | `createTaskChecklistItem` | Yangi checklist bandi qo'shish | ✅ Faol |
| 50 | Checklists | `PUT` | `/api/tasks/{taskId}/checklists/{itemId}` | `updateTaskChecklistItem` | Checklist bandini yangilash (sarlavha yoki bajarilganlik) | ✅ Faol |
| 51 | Checklists | `DELETE` | `/api/tasks/{taskId}/checklists/{itemId}` | `deleteTaskChecklistItem` | Checklist bandini o'chirish | ✅ Faol |
| 52 | Checklists | `GET` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists` | `getWorkspaceTaskChecklistsLegacy` | Taskning barcha checklistlarini olish (eski uslub) | ⚠️ Deprecated |
| 53 | Checklists | `POST` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists` | `createWorkspaceTaskChecklistItemLegacy` | Yangi checklist qo'shish (eski uslub) | ⚠️ Deprecated |
| 54 | Checklists | `PUT` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists/{itemId}` | `updateWorkspaceTaskChecklistItemLegacy` | Checklistni yangilash (eski uslub) | ⚠️ Deprecated |
| 55 | Checklists | `DELETE` | `/api/workspaces/{workspaceId}/tasks/{taskId}/checklists/{itemId}` | `deleteWorkspaceTaskChecklistItemLegacy` | Checklistni o'chirish (eski uslub) | ⚠️ Deprecated |
| 56 | Columns | `GET` | `/api/workspaces/{workspaceId}/columns` | `getWorkspaceColumns` | Workspace ga tegishli ustunlar ro'yxatini olish | ✅ Faol |
| 57 | Columns | `POST` | `/api/workspaces/{workspaceId}/columns` | `createWorkspaceColumn` | Yangi ustun yaratish | ✅ Faol |
| 58 | Columns | `PATCH` | `/api/workspaces/{workspaceId}/columns` | `reorderWorkspaceColumnsLegacy` | Ustunlar tartibini o'zgartirish (eski variant) | ⚠️ Deprecated |
| 59 | Columns | `PATCH` | `/api/workspaces/{workspaceId}/columns/reorder` | `reorderWorkspaceColumns` | Ustunlar tartibini ID lar ro'yxati orqali yangilash (oddiy massiv: ['col1', 'col2']) | ✅ Faol |
| 60 | Columns | `PUT` | `/api/workspaces/{workspaceId}/columns/{id}` | `updateWorkspaceColumn` | Ustunni to'liq yangilash | ✅ Faol |
| 61 | Columns | `DELETE` | `/api/workspaces/{workspaceId}/columns/{id}` | `deleteWorkspaceColumn` | Ustunni o'chirish | ✅ Faol |
| 62 | Columns | `PATCH` | `/api/workspaces/{workspaceId}/columns/{id}` | `patchWorkspaceColumn` | Ustunni qisman yangilash | ✅ Faol |
| 63 | Comments | `GET` | `/api/tasks/{taskId}/comments` | `getTaskComments` | Task izohlari ro'yxatini sahifalab olish | ✅ Faol |
| 64 | Comments | `POST` | `/api/tasks/{taskId}/comments` | `createTaskComment` | Taskka yangi izoh qo'shish | ✅ Faol |
| 65 | Comments | `PUT` | `/api/tasks/{taskId}/comments/{commentId}` | `updateTaskComment` | Izohni tahrirlash | ✅ Faol |
| 66 | Comments | `DELETE` | `/api/tasks/{taskId}/comments/{commentId}` | `deleteTaskComment` | Izohni o'chirish | ✅ Faol |
| 67 | Config | `GET` | `/api/v1/config/public` | `getPublicSystemConfig` | Get public config | ✅ Faol |
| 68 | Contacts | `GET` | `/api/contacts` | `getMyContacts` | Foydalanuvchining shaxsiy kontaktlari ro'yxatini olish | ✅ Faol |
| 69 | Contacts | `POST` | `/api/contacts` | `addContact` | Login nomi yoki email orqali yangi kontakt qo'shish | ✅ Faol |
| 70 | Contacts | `DELETE` | `/api/contacts/{contactUserId}` | `deleteContact` | Kontaktni ro'yxatdan o'chirish | ✅ Faol |
| 71 | Directions | `GET` | `/api/workspaces/{workspaceId}/directions` | `getWorkspaceDirections` | Workspace ga tegishli barcha yo'nalishlarni olish | ✅ Faol |
| 72 | Directions | `POST` | `/api/workspaces/{workspaceId}/directions` | `createWorkspaceDirection` | Yangi yo'nalish qo'shish | ✅ Faol |
| 73 | Directions | `PUT` | `/api/workspaces/{workspaceId}/directions/{id}` | `updateWorkspaceDirection` | Yo'nalishni tahrirlash (nomi, rangi) | ✅ Faol |
| 74 | Directions | `DELETE` | `/api/workspaces/{workspaceId}/directions/{id}` | `deleteWorkspaceDirection` | Yo'nalishni o'chirish (Faqat OWNER/ADMIN) | ✅ Faol |
| 75 | Export | `GET` | `/api/workspaces/{workspaceId}/export/csv` | `exportWorkspaceTasksToCsv` | Workspace dagi barcha vazifalarni CSV formatida yuklash | ✅ Faol |
| 76 | Export | `GET` | `/api/workspaces/{workspaceId}/export/daily-report.pdf` | `exportWorkspaceDailyReportPdf` | Workspace ning kunlik hisobotini PDF formatida yuklash | ✅ Faol |
| 77 | Fayllar (Files) | `GET` | `/api/files/chat-attachments/{fileName}` | `downloadChatAttachmentFile` | Chat biriktirmasini yuklab olish yoki ko'rish | ✅ Faol |
| 78 | Fayllar (Files) | `POST` | `/api/files/upload` | `uploadGeneralFile` | Fayl yuklash (xabar biriktirmasi uchun) | ✅ Faol |
| 79 | Fayllar (Files) | `GET` | `/api/files/{fileName}` | `downloadGeneralFile` | Faylni yuklab olish yoki ko'rish | ✅ Faol |
| 80 | Focus Timers | `GET` | `/api/timers/active` | `getActiveTimer` | Joriy foydalanuvchining faol taymerini olish | ✅ Faol |
| 81 | Focus Timers | `POST` | `/api/timers/start` | `startTimer` | Pomodoro taymerini boshlash | ✅ Faol |
| 82 | Focus Timers | `POST` | `/api/timers/stop` | `stopTimer` | Faol Pomodoro taymerini to'xtatish va ishlangan vaqtni yozish | ✅ Faol |
| 83 | Labels | `GET` | `/api/workspaces/{workspaceId}/labels` | `getWorkspaceLabels` | Workspace ga tegishli barcha labellarni olish | ✅ Faol |
| 84 | Labels | `POST` | `/api/workspaces/{workspaceId}/labels` | `createWorkspaceLabel` | Yangi label yaratish | ✅ Faol |
| 85 | Labels | `PUT` | `/api/workspaces/{workspaceId}/labels/{id}` | `updateWorkspaceLabel` | Label ni yangilash | ✅ Faol |
| 86 | Labels | `DELETE` | `/api/workspaces/{workspaceId}/labels/{id}` | `deleteWorkspaceLabel` | Label ni o'chirish | ✅ Faol |
| 87 | Notifications | `GET` | `/api/notifications` | `getMyNotifications` | Mening bildirishnomalarim ro'yxatini olish (Pageable) | ✅ Faol |
| 88 | Notifications | `PUT` | `/api/notifications/read-all` | `markAllNotificationsAsRead` | Barcha o'qilmaganlarni o'qilgan deb belgilash | ✅ Faol |
| 89 | Notifications | `PUT` | `/api/notifications/{id}/read` | `markNotificationAsRead` | Bildirishnomani o'qilgan deb belgilash | ✅ Faol |
| 90 | Reports | `GET` | `/api/workspaces/{workspaceId}/reports/sprints/{sprintId}` | `getSprintSummaryReport` | Muayyan Sprint bo'yicha ishlash tezligi (Burn-down) hisobotini olish | ✅ Faol |
| 91 | Reports | `GET` | `/api/workspaces/{workspaceId}/reports/summary` | `getWorkspaceSummaryReport` | Loyiha bo'yicha umumiy hisobot va xodimlar ish yukini olish | ✅ Faol |
| 92 | Sprints | `GET` | `/api/workspaces/{workspaceId}/backlog` | `getWorkspaceBacklog` | Workspace Backlog vazifalari ro'yxatini olish (sprintsiz tasklar) | ✅ Faol |
| 93 | Sprints | `GET` | `/api/workspaces/{workspaceId}/sprints` | `listWorkspaceSprints` | Workspacedagi sprintlar ro'yxatini olish (status bo'yicha filter qilish mumkin) | ✅ Faol |
| 94 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints` | `createWorkspaceSprint` | Yangi sprint yaratish | ✅ Faol |
| 95 | Sprints | `GET` | `/api/workspaces/{workspaceId}/sprints/{id}` | `getWorkspaceSprintById` | Sprint ma'lumotlarini id bo'yicha olish | ✅ Faol |
| 96 | Sprints | `PUT` | `/api/workspaces/{workspaceId}/sprints/{id}` | `updateWorkspaceSprint` | Sprintni tahrirlash | ✅ Faol |
| 97 | Sprints | `DELETE` | `/api/workspaces/{workspaceId}/sprints/{id}` | `deleteWorkspaceSprint` | Sprintni o'chirish (vazifalar backlogga qaytariladi) | ✅ Faol |
| 98 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints/{id}/complete` | `completeWorkspaceSprint` | Sprintni yakunlash (COMPLETED holatiga o'tkazish) | ✅ Faol |
| 99 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints/{id}/start` | `startWorkspaceSprint` | Sprintni boshlash (ACTIVE holatiga o'tkazish) | ✅ Faol |
| 100 | Sprints | `GET` | `/api/workspaces/{workspaceId}/sprints/{id}/tasks` | `getSprintTasks` | Sprintdagi barcha vazifalarni olish | ✅ Faol |
| 101 | Sprints | `POST` | `/api/workspaces/{workspaceId}/sprints/{id}/tasks` | `addTasksToSprint` | Vazifalarni sprintga biriktirish | ✅ Faol |
| 102 | Sprints | `DELETE` | `/api/workspaces/{workspaceId}/sprints/{id}/tasks/{taskId}` | `removeTaskFromSprint` | Vazifani sprintdan chiqarib backlogga qaytarish | ✅ Faol |
| 103 | Task Activities | `GET` | `/api/tasks/{taskId}/activities` | `getTaskActivities` | Task o'zgarishlar tarixini sahifalab olish (History) | ✅ Faol |
| 104 | Task Activities | `GET` | `/api/workspaces/{workspaceId}/activities` | `getWorkspaceActivities` | Workspace bo'yicha barcha vazifalar o'zgarishlar tarixini (Activity Feed) sahifalab olish | ✅ Faol |
| 105 | Task Dependencies | `GET` | `/api/tasks/{taskId}/dependencies` | `getDependencies` | Vazifaning bog'liqliklarini olish | ✅ Faol |
| 106 | Task Dependencies | `POST` | `/api/tasks/{taskId}/dependencies` | `addDependency` | Vazifaga yangi to'siq (Blocker) bog'liqlik qo'shish | ✅ Faol |
| 107 | Task Dependencies | `DELETE` | `/api/tasks/{taskId}/dependencies/{dependencyId}` | `removeDependency` | Bog'liqlikni o'chirish | ✅ Faol |
| 108 | Tasks | `POST` | `/api/v2/workspaces/{workspaceId}/tasks` | `createTaskV2` | Yangi vazifa yaratish (v2) | ✅ Faol |
| 109 | Tasks | `GET` | `/api/workspaces/{workspaceId}/tasks` | `listWorkspaceTasks` | Workspace ga tegishli vazifalar ro'yxatini olish (qidiruv, filter va pagination bilan) | ✅ Faol |
| 110 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks` | `createWorkspaceTask` | Yangi vazifa yaratish | ✅ Faol |
| 111 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/columns/{columnId}/rebalance` | `rebalanceColumnLexoRanks` | Ustundagi barcha vazifalar LexoRank tartibini qayta muvozanatlash (Rebalance) | ✅ Faol |
| 112 | Tasks | `GET` | `/api/workspaces/{workspaceId}/tasks/{id}` | `getWorkspaceTaskById` | Bitta vazifani ID orqali olish (batafsil ma'lumotlari bilan) | ✅ Faol |
| 113 | Tasks | `PUT` | `/api/workspaces/{workspaceId}/tasks/{id}` | `updateWorkspaceTask` | Vazifani to'liq yangilash | ✅ Faol |
| 114 | Tasks | `DELETE` | `/api/workspaces/{workspaceId}/tasks/{id}` | `deleteWorkspaceTask` | Vazifani o'chirish | ✅ Faol |
| 115 | Tasks | `PATCH` | `/api/workspaces/{workspaceId}/tasks/{id}` | `patchWorkspaceTask` | Vazifani qisman yangilash | ✅ Faol |
| 116 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/archive` | `archiveWorkspaceTask` | Vazifani arxivga olish | ✅ Faol |
| 117 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/assign` | `toggleTaskAssignee` | Vazifaga foydalanuvchini biriktirish / olib tashlash | ✅ Faol |
| 118 | Tasks | `PATCH` | `/api/workspaces/{workspaceId}/tasks/{id}/reorder` | `reorderWorkspaceTask` | Vazifaning o'rnini (rank) va ustunini yangilash (Lexorank) | ✅ Faol |
| 119 | Tasks | `GET` | `/api/workspaces/{workspaceId}/tasks/{id}/subtasks` | `getTaskSubtasks` | Ota vazifaga biriktirilgan ostki vazifalar (Subtasks) ro'yxatini olish | ✅ Faol |
| 120 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/unarchive` | `unarchiveWorkspaceTask` | Vazifani arxivdan chiqarish | ✅ Faol |
| 121 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/watch` | `toggleTaskWatch` | Vazifani kuzatish / kuzatishni to'xtatish (Watch) | ✅ Faol |
| 122 | Tasks | `GET` | `/api/workspaces/{workspaceId}/tasks/{id}/worklogs` | `getTaskWorklogs` | Vazifa bo'yicha sarflangan ish vaqti (Worklogs) tarixini olish | ✅ Faol |
| 123 | Tasks | `POST` | `/api/workspaces/{workspaceId}/tasks/{id}/worklogs` | `addTaskWorklog` | Vazifaga sarflangan ish vaqtini (Worklog) qo'shish | ✅ Faol |
| 124 | Tasks Direct | `GET` | `/api/tasks/{id}` | `getTaskDirectById` | Vazifani bevosita ID orqali olish (eski/dublikat endpoint) | ⚠️ Deprecated |
| 125 | Tasks Direct | `GET` | `/api/tasks/{id}/details` | `getTaskDetails` | Vazifa to'liq ma'lumotlarini olish | ⚠️ Deprecated |
| 126 | Telegram Auth | `POST` | `/api/auth/telegram` | `loginWithTelegram` | Telegram Mini App orqali login (Tavsiya etilgan usul) | ✅ Faol |
| 127 | Telegram Auth | `POST` | `/api/auth/telegram/pending-invite` | `createPendingTelegramInvite_1` | Mini App ulanmagan foydalanuvchi uchun kutilayotgan taklif yaratish | ✅ Faol |
| 128 | Telegram Auth | `POST` | `/api/v1/auth/telegram` | `loginWithTelegram_1` | Telegram Mini App orqali login (Tavsiya etilgan usul) | ✅ Faol |
| 129 | Telegram Auth | `POST` | `/api/v1/auth/telegram/pending-invite` | `createPendingTelegramInvite` | Mini App ulanmagan foydalanuvchi uchun kutilayotgan taklif yaratish | ✅ Faol |
| 130 | Users | `GET` | `/api/auth/me` | `getCurrentUserLegacy` | Joriy foydalanuvchi profilini olish (eski alias) | ⚠️ Deprecated |
| 131 | Users | `GET` | `/api/me` | `getCurrentUser` | Joriy foydalanuvchi profilini olish | ✅ Faol |
| 132 | Users | `GET` | `/api/me/tasks` | `getMyAssignedTasksLegacy` | Joriy foydalanuvchiga biriktirilgan vazifalar (eski alias) | ⚠️ Deprecated |
| 133 | Users | `GET` | `/api/users` | `getWorkspaceUsers` | Workspace dagi foydalanuvchilar ro'yxatini olish | ✅ Faol |
| 134 | Users | `PATCH` | `/api/users/me` | `updateCurrentUserProfile` | Profil ma'lumotlarini tahrirlash | ✅ Faol |
| 135 | Users | `POST` | `/api/users/me/change-password` | `changePassword` | Parolni o'zgartirish | ✅ Faol |
| 136 | Users | `GET` | `/api/users/me/tasks` | `getMyAssignedTasks` | Joriy foydalanuvchiga biriktirilgan barcha vazifalar ro'yxatini olish (barcha workspacelar bo'yicha) | ✅ Faol |
| 137 | Users | `POST` | `/api/users/me/telegram` | `linkTelegram` | Mini App orqali Telegram akkauntni ulash | ✅ Faol |
| 138 | Users | `DELETE` | `/api/users/me/telegram` | `unlinkTelegram` | Telegram akkauntni uzish (faqat joriy foydalanuvchi uchun) | ✅ Faol |
| 139 | Users | `GET` | `/api/users/me/telegram-link-token` | `getTelegramLinkToken` | Telegram bilan ulash uchun vaqtinchalik token, tayyor havola va amal qilish muddatini olish | ✅ Faol |
| 140 | Workspace | `GET` | `/api/workspaces` | `getMyWorkspaces` | Foydalanuvchining workspace lari ro'yxatini olish | ✅ Faol |
| 141 | Workspace | `POST` | `/api/workspaces` | `createWorkspace` | Yangi workspace yaratish | ✅ Faol |
| 142 | Workspace | `GET` | `/api/workspaces/{id}` | `getWorkspaceById` | Bitta workspace ma'lumotlarini ID orqali olish | ✅ Faol |
| 143 | Workspace | `PUT` | `/api/workspaces/{id}` | `updateWorkspace` | Mavjud workspace ni tahrirlash | ✅ Faol |
| 144 | Workspace | `DELETE` | `/api/workspaces/{id}` | `deleteWorkspace` | Workspace ni o'chirish | ✅ Faol |
| 145 | Workspace Invitations | `POST` | `/api/invitations/by-id/{id}/accept` | `acceptWorkspaceInvitationById` | Taklifni ID bo'yicha qabul qilish (/me ro'yxatidan) | ✅ Faol |
| 146 | Workspace Invitations | `POST` | `/api/invitations/by-id/{id}/reject` | `rejectWorkspaceInvitationById` | Taklifni ID bo'yicha rad etish (/me ro'yxatidan) | ✅ Faol |
| 147 | Workspace Invitations | `GET` | `/api/invitations/me` | `getMyPendingInvitations` | Menga kelgan faol takliflarni ko'rish | ✅ Faol |
| 148 | Workspace Invitations | `PATCH` | `/api/invitations/{id}/accept` | `acceptWorkspaceInvitationLegacy` | Taklifni qabul qilish (Legacy PATCH) | ✅ Faol |
| 149 | Workspace Invitations | `GET` | `/api/invitations/{id}/details` | `getInvitationDetails` | Taklifnoma to'liq ma'lumotlarini olish (Ichki/Batafsil) | ✅ Faol |
| 150 | Workspace Invitations | `PATCH` | `/api/invitations/{id}/reject` | `rejectWorkspaceInvitationLegacy` | Taklifni rad etish (Legacy PATCH) | ✅ Faol |
| 151 | Workspace Invitations | `GET` | `/api/invitations/{token}` | `getPublicInvitationPreview` | Ommaviy taklifnoma ma'lumotlarini olish (Minimal/Xavfsiz) | ✅ Faol |
| 152 | Workspace Invitations | `POST` | `/api/invitations/{token}/accept` | `acceptWorkspaceInvitationByToken` | Taklifni qabul qilish | ✅ Faol |
| 153 | Workspace Invitations | `POST` | `/api/invitations/{token}/reject` | `rejectWorkspaceInvitationByToken` | Taklifni rad etish | ✅ Faol |
| 154 | Workspace Invitations | `GET` | `/api/workspaces/{workspaceId}/invites` | `getWorkspaceInvitations` | Workspace takliflarini ko'rish (Admin/Owner) | ✅ Faol |
| 155 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites` | `createWorkspaceEmailInvitation` | Email orqali taklif yuborish (Admin/Owner) | ✅ Faol |
| 156 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/link` | `createWorkspaceLinkInvitation` | Havola orqali taklif yaratish (Admin/Owner) | ✅ Faol |
| 157 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/link/regenerate` | `regenerateWorkspaceLinkInvitation` | Taklif havolasini yangilash (Admin/Owner) | ✅ Faol |
| 158 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/telegram-link` | `createTelegramInviteLink` | Telegram taklif havolasini yaratish (Admin/Owner) | ✅ Faol |
| 159 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/test-email` | `testBrevoEmailSending` | Brevo email yuborilishini tekshirish (Admin/Owner) | ✅ Faol |
| 160 | Workspace Invitations | `DELETE` | `/api/workspaces/{workspaceId}/invites/{id}` | `cancelWorkspaceInvitation` | Taklifni bekor qilish (Admin/Owner) | ✅ Faol |
| 161 | Workspace Invitations | `POST` | `/api/workspaces/{workspaceId}/invites/{id}/resend` | `resendWorkspaceEmailInvitation` | Email taklifnomani qayta yuborish (Admin/Owner) | ✅ Faol |
| 162 | Workspace Members | `GET` | `/api/workspaces/{workspaceId}/members` | `getWorkspaceMembers` | Workspace a'zolari ro'yxatini rollari bilan olish | ✅ Faol |
| 163 | Workspace Members | `POST` | `/api/workspaces/{workspaceId}/members` | `inviteWorkspaceMember` | Workspace ga yangi a'zo taklif qilish / qo'shish | ✅ Faol |
| 164 | Workspace Members | `DELETE` | `/api/workspaces/{workspaceId}/members/{userId}` | `removeWorkspaceMember` | Workspace a'zosini chiqarib yuborish yoki jamoani tark etish | ✅ Faol |
| 165 | Workspace Members | `PATCH` | `/api/workspaces/{workspaceId}/members/{userId}` | `updateWorkspaceMemberRole` | Workspace a'zosining rolini o'zgartirish | ✅ Faol |
