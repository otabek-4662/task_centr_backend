package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_tasks_workspace_order", columnList = "workspace_id, task_order"),
    @Index(name = "idx_tasks_column_order", columnList = "column_id, task_order"),
    @Index(name = "idx_tasks_public_id", columnList = "public_id", unique = true),
    @Index(name = "idx_tasks_workspace_priority", columnList = "workspace_id, priority"),
    @Index(name = "idx_tasks_workspace_due_date", columnList = "workspace_id, due_date"),
    @Index(name = "idx_tasks_workspace_issue_type", columnList = "workspace_id, issue_type"),
    @Index(name = "idx_tasks_sprint_id", columnList = "sprint_id"),
    @Index(name = "idx_tasks_workspace_sprint", columnList = "workspace_id, sprint_id")
})
@SQLDelete(sql = "UPDATE tasks SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
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

    @Version
    private Long version;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 32)
    @Builder.Default
    private IssueType issueType = IssueType.TASK;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Column(name = "sprint_id")
    private String sprintId;

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany
    @org.hibernate.annotations.BatchSize(size = 25)
    @JoinTable(
        name = "task_labels",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    @Builder.Default
    private Set<Label> labels = new HashSet<>();

    @ManyToMany
    @org.hibernate.annotations.BatchSize(size = 25)
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
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.publicId == null) {
            this.publicId = "WFM-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        }
    }
}
