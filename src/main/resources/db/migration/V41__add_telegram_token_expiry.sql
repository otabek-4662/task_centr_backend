-- V41: Telegram link token uchun muddati tugash maydoni qo'shish
-- Avvalgi 6 xonali, muddatsiz token o'rniga 8 belgili, 15 daqiqa amal qiladigan token
ALTER TABLE users ADD COLUMN IF NOT EXISTS telegram_link_token_expires_at TIMESTAMP(6);
