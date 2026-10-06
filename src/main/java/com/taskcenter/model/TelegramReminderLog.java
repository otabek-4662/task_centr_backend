package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "telegram_reminder_log",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_reminder_task_user_type", columnNames = {"task_id", "user_id", "type"})
    },
    indexes = {
        @Index(name = "idx_reminder_log_task", columnList = "task_id"),
        @Index(name = "idx_reminder_log_user", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private ReminderType type;

    @Column(name = "sent_at", nullable = false)
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    public enum ReminderType {
        /** @deprecated Eski tip, bazadagi mavjud yozuvlar uchun saqlanadi */
        H24,
        /** @deprecated Eski tip, bazadagi mavjud yozuvlar uchun saqlanadi */
        H1,
        /** @deprecated Eski tip, bazadagi mavjud yozuvlar uchun saqlanadi */
        OVERDUE,
        /** Ertangi muddatli tasklar uchun eslatma */
        DUE_TOMORROW,
        /** Global background job lock */
        GLOBAL_JOB_LOCK
    }
}
