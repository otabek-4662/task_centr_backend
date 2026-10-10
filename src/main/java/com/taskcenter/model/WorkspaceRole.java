package com.taskcenter.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ish maydonidagi foydalanuvchi roli: OWNER (asoschi), ADMIN (administrator), MEMBER (a'zo), VIEWER (kuzatuvchi)")
public enum WorkspaceRole {
    OWNER("G'alvaning asoschisi"),
    ADMIN("Zavxoz"),
    MEMBER("Qora ishchi"),
    VIEWER("Tomoshabin");

    private final String displayName;

    WorkspaceRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
