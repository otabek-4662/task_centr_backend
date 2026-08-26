package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "board_columns")
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

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        }
    }
}
