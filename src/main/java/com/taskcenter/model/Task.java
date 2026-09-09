package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_tasks_workspace_order", columnList = "workspace_id, task_order"),
    @Index(name = "idx_tasks_column_order", columnList = "column_id, task_order"),
    @Index(name = "idx_tasks_public_id", columnList = "public_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String publicId;

    @Column(nullable = false)
    private String workspaceId;

    @Column(nullable = false)
    private String columnId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "task_order", nullable = false)
    private Integer order;

    @ManyToMany
    @JoinTable(
        name = "task_labels",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    @Builder.Default
    private Set<Label> labels = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "task_assignees",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> assignees = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        if (this.publicId == null) {
            this.publicId = "WFM-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        }
    }
}
