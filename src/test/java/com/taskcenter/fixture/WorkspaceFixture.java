package com.taskcenter.fixture;

import com.taskcenter.model.Workspace;

import java.util.UUID;

/**
 * RealWorld Test Data Builder naqshi bo'yicha Workspace obyektlarini yaratish fixturasi.
 */
public class WorkspaceFixture {

    public static WorkspaceBuilder aWorkspace() {
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        return new WorkspaceBuilder()
                .id(UUID.randomUUID().toString())
                .title("Workspace " + randomSuffix)
                .bgColor("#1a1b41")
                .description("Test workspace description " + randomSuffix)
                .keyPrefix("WS" + randomSuffix.substring(0, 3).toUpperCase());
    }

    public static class WorkspaceBuilder {
        private String id;
        private String title;
        private String bgColor;
        private String description;
        private String keyPrefix;
        private String ownerId;

        public WorkspaceBuilder id(String id) {
            this.id = id;
            return this;
        }

        public WorkspaceBuilder title(String title) {
            this.title = title;
            return this;
        }

        public WorkspaceBuilder bgColor(String bgColor) {
            this.bgColor = bgColor;
            return this;
        }

        public WorkspaceBuilder description(String description) {
            this.description = description;
            return this;
        }

        public WorkspaceBuilder keyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
            return this;
        }

        public WorkspaceBuilder ownerId(String ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Workspace build() {
            return Workspace.builder()
                    .id(id != null ? id : UUID.randomUUID().toString())
                    .title(title)
                    .bgColor(bgColor)
                    .description(description)
                    .keyPrefix(keyPrefix)
                    .ownerId(ownerId)
                    .build();
        }
    }
}
