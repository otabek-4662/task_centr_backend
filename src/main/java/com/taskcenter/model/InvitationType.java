package com.taskcenter.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Taklif turi: EMAIL (aniq elektron pochtaga yuborilgan) yoki LINK (umumiy havola orqali)")
public enum InvitationType {
    EMAIL,
    LINK
}
