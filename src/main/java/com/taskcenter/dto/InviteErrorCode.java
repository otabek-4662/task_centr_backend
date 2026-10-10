package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Taklif tizimiga oid standart xato kodlari (Mashina o'qiy oladigan enum)")
public enum InviteErrorCode {
    INVITE_NOT_FOUND,
    INVITE_EXPIRED,
    INVITE_CANCELLED,
    INVITE_REJECTED,
    INVITE_ALREADY_USED,
    INVITE_LIMIT_REACHED,
    INVITE_EMAIL_MISMATCH,
    INVITE_DUMMY_EMAIL,
    ALREADY_MEMBER,
    DUPLICATE_PENDING_INVITE,
    CANNOT_INVITE_OWNER,
    RATE_LIMITED,
    TEST_EMAIL_LIMIT,
    FORBIDDEN_ROLE,
    INVALID_INVITE_TYPE,
    INVITE_NOT_FOR_USER,
    VALIDATION_ERROR
}
