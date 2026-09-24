-- V23: Foydalanuvchi oxirgi faollik vaqtini kuzatish (Online status)

ALTER TABLE users ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMP;
