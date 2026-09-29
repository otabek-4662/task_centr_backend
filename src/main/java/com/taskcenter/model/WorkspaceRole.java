package com.taskcenter.model;

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
