package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "board_columns", indexes = {
    @Index(name = "idx_board_columns_workspace_order", columnList = "workspace_id, column_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardColumn {

    @Id
    private String id;

    @Column(nullable = false)
    private String workspaceId;

    @Column(nullable = false)
    private String title;

    @Column(name = "column_order", nullable = false)
    private Integer order;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @OneToMany(mappedBy = "columnId", fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.Set<Task> tasks = new java.util.HashSet<>();

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }
}
