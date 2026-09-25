-- WorkspaceMember dagi qidiruvni tezlashtirish
CREATE INDEX IF NOT EXISTS idx_workspace_members_user_id ON workspace_members (user_id);

-- Foydalanuvchilarni ism yoki email orqali izlashni tezlashtirish
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_name ON users (name);

-- Sprintlarni sanasiga ko'ra saralab olishni tezlashtirish
CREATE INDEX IF NOT EXISTS idx_sprints_workspace_created ON sprints (workspace_id, created_at DESC);
