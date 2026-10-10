package com.taskcenter.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Elektron pochta yetkazish holati: PENDING (yuborilmoqda), SENT (muvaffaqiyatli yetkazildi), FAILED (xatolik), SKIPPED (o'tkazib yuborildi), NOT_APPLICABLE (LINK turida yoki Telegram orqali ochilganda)")
public enum EmailDeliveryStatus {
    PENDING,
    SENT,
    FAILED,
    SKIPPED,
    NOT_APPLICABLE
}
