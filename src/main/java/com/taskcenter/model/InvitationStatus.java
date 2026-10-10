package com.taskcenter.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Taklif holati: PENDING (kutilmoqda), ACCEPTED (qabul qilingan), REJECTED (rad etilgan), CANCELLED (bekor qilingan), EXPIRED (muddati o'tgan)")
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    EXPIRED
}
