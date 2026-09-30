-- V40: Performance indexes — tez-tez ishlatiladigan filter va join ustunlariga qo'shimcha indekslar

-- workspace_invitations: status bo'yicha filter tez-tez ishlatiladi (pending count, pending list)
CREATE INDEX IF NOT EXISTS idx_wi_workspace_status
    ON workspace_invitations (workspace_id, status);

-- workspace_invitations: receiver_email bo'yicha tekshirish (addMember da)
CREATE INDEX IF NOT EXISTS idx_wi_receiver_email_status
    ON workspace_invitations (receiver_email, status)
    WHERE receiver_email IS NOT NULL;

-- task_watchers: task_id bo'yicha qidiruv uchun
CREATE INDEX IF NOT EXISTS idx_task_watchers_task_fk
    ON task_watchers (task_id);

CREATE INDEX IF NOT EXISTS idx_task_watchers_user_fk
    ON task_watchers (user_id);

-- task_assignees: foydalanuvchiga tegishli tasklar sonini hisoblash uchun covering index
-- countAssignedTasksByUserId (@UserService) da ishlatiladi
CREATE INDEX IF NOT EXISTS idx_task_assignees_user_task
    ON task_assignees (user_id, task_id);

-- workspace_members: (workspace_id, user_id) composite — checkAccess da findByWorkspaceIdAndUserId uchun
-- Bu PK bo'lgani uchun allaqachon mavjud, lekin role ni ham cover qiluvchi index
CREATE INDEX IF NOT EXISTS idx_wm_workspace_user_role
    ON workspace_members (workspace_id, user_id, role);

-- comments: task bo'yicha izoh olib kelish uchun
CREATE INDEX IF NOT EXISTS idx_comments_task_created
    ON comments (task_id, created_at DESC);

-- task_activities: task log uchun
CREATE INDEX IF NOT EXISTS idx_task_activities_task_created
    ON task_activities (task_id, created_at DESC);
