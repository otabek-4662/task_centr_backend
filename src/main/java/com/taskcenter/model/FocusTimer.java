package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "focus_timers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FocusTimer {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "task_id", length = 36)
    private String taskId;

    @Column(name = "target_minutes", nullable = false)
    @Builder.Default
    private Integer targetMinutes = 25;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "logged_hours_added")
    @Builder.Default
    private Double loggedHoursAdded = 0.0;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.targetMinutes == null) {
            this.targetMinutes = 25;
        }
        if (this.loggedHoursAdded == null) {
            this.loggedHoursAdded = 0.0;
        }
    }
}
