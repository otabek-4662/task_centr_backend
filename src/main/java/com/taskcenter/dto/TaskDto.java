package com.taskcenter.dto;

import com.taskcenter.model.Direction;
import com.taskcenter.model.IssueType;
import com.taskcenter.model.Label;
import com.taskcenter.model.Priority;
import com.taskcenter.model.Task;
import com.taskcenter.model.TaskChecklistItem;
import com.taskcenter.model.User;
import com.taskcenter.util.AppTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {
    @Schema(description = "Vazifaning unikal identifikatori (UUID)", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String id;

    @Schema(description = "Qisqa inson o'qiy oladigan identifikator", example = "PROJ-12")
    private String publicId;

    @Schema(description = "Vazifaning qisqa kodi/kaliti (masalan WFM-204)", example = "WFM-204")
    private String key;

    @Schema(description = "Vazifaning qisqa kodi (masalan WFM-204)", example = "WFM-204")
    private String code;

    @Schema(description = "Ustun identifikatori", example = "col-12345")
    private String columnId;

    @Schema(description = "Ishchi maydon identifikatori", example = "ws-12345")
    private String workspaceId;

    @Schema(description = "Ota vazifa (Parent Task) identifikatori (Sub-task uchun)", example = "task-parent-123")
    private String parentId;

    @Schema(description = "Vazifa sarlavhasi", example = "Swagger UI integratsiyasini ulash")
    private String title;

    @Schema(description = "Vazifa batafsil tavsifi", example = "Frontendda Swagger UI guruhlarini ko'rsatish")
    private String description;

    @Schema(description = "LexoRank saralash tartibi", example = "0|hzzzzz:")
    private String lexoRank;

    @Schema(description = "Vazifaning rank tartibi (LexoRank)", example = "0|hzzzzz:")
    private String rank;

    @Schema(description = "Muhimlik darajasi", example = "HIGH")
    private Priority priority;

    @Schema(description = "Vazifa turi", example = "TASK")
    private IssueType issueType;

    @Schema(description = "Muddati (YYYY-MM-DD)", example = "2026-10-15")
    private LocalDate dueDate;

    @Schema(description = "Story Points (Agile bahosi)", example = "5")
    private Integer storyPoints;

    @Schema(description = "Taxmin qilingan soatlar", example = "4.5")
    private Double estimatedHours;

    @Schema(description = "Sarflangan soatlar", example = "2.0")
    private Double loggedHours;

    @Schema(description = "Muddati o'tib ketganmi", example = "false")
    private Boolean isOverdue;

    @Schema(description = "Checklist elementlarining umumiy soni", example = "4")
    private Integer totalChecklistItems;

    @Schema(description = "Bajarilgan checklist elementlari soni", example = "2")
    private Integer completedChecklistItems;

    @Schema(description = "Biriktirilgan sprint ID si", example = "sprint-67890")
    private String sprintId;

    @Schema(description = "Vazifaga biriktirilgan teglar")
    private List<LabelDto> labels;

    @Schema(description = "Frontend kartochkalari uchun teg/yo'nalish nomlari ro'yxati")
    private List<String> labelNames;

    @Schema(description = "Vazifaga biriktirilgan yo'nalishlar (Frontend, Backend, QA va h.k.)")
    private List<DirectionDto> directions;

    @Schema(description = "Vazifaga biriktirilgan ijrochilar")
    private List<UserDto> assignees;

    @Schema(description = "Frontend kartochkalari uchun biriktirilgan ijrochilar ismlari ro'yxati")
    private List<String> members;

    @Schema(description = "Vazifaga biriktirilgan asosiy ijrochi", implementation = UserSummaryDto.class)
    private UserSummaryDto assignee;

    @Schema(description = "Vazifani yaratgan/hisob beruvchi foydalanuvchi", implementation = ReporterDto.class)
    private ReporterDto reporter;

    @Schema(description = "Kuzatuvchilar ro'yxati")
    private List<UserDto> watchers;

    @Schema(description = "Arxivlanganmi", example = "false")
    private Boolean isArchived;

    @Schema(description = "Yaratilgan vaqt (ISO-8601)", example = "2026-10-08T15:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "So'nggi o'zgartirilgan vaqt (ISO-8601)", example = "2026-10-08T15:35:00")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummaryDto {
        @Schema(description = "Foydalanuvchi identifikatori", example = "u-123456")
        private String id;

        @Schema(description = "Foydalanuvchi to'liq ismi", example = "Ali Valiyev")
        private String fullName;

        @Schema(description = "Avatar havolasi", example = "https://example.com/avatar.png")
        private String avatarUrl;

        public static UserSummaryDto fromEntity(User user) {
            if (user == null) return null;
            return UserSummaryDto.builder()
                    .id(user.getId())
                    .fullName(user.getFullName() != null ? user.getFullName() : user.getName())
                    .avatarUrl(null)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReporterDto {
        @Schema(description = "Hisob beruvchi identifikatori", example = "u-123456")
        private String id;

        @Schema(description = "Hisob beruvchi to'liq ismi", example = "Vali Aliyev")
        private String fullName;

        public static ReporterDto fromEntity(User user) {
            if (user == null) return null;
            return ReporterDto.builder()
                    .id(user.getId())
                    .fullName(user.getFullName() != null ? user.getFullName() : user.getName())
                    .build();
        }
    }

    public static TaskDto fromEntity(Task t) {
        return fromEntity(t, null);
    }

    public static TaskDto fromEntity(Task t, User reporterUser) {
        if (t == null) return null;

        User firstAssignee = (t.getAssignees() != null && !t.getAssignees().isEmpty())
                ? t.getAssignees().iterator().next()
                : null;

        ReporterDto reporterDto = null;
        if (reporterUser != null) {
            reporterDto = ReporterDto.fromEntity(reporterUser);
        } else if (t.getCreatedBy() != null) {
            reporterDto = ReporterDto.builder()
                    .id(t.getCreatedBy())
                    .fullName(t.getCreatedBy())
                    .build();
        }

        String taskCode = t.getPublicId() != null ? t.getPublicId() : t.getId();

        List<String> memberNames = t.getAssignees() != null
                ? t.getAssignees().stream()
                        .map(u -> (u.getFullName() != null && !u.getFullName().isBlank()) ? u.getFullName() : u.getName())
                        .filter(n -> n != null && !n.isBlank())
                        .collect(Collectors.toList())
                : List.of();

        List<String> combinedLabelNames = new ArrayList<>();
        if (t.getDirections() != null) {
            for (Direction d : t.getDirections()) {
                if (d.getName() != null && !d.getName().isBlank()) {
                    combinedLabelNames.add(d.getName().trim().toLowerCase());
                }
            }
        }
        if (t.getLabels() != null) {
            for (Label l : t.getLabels()) {
                if (l.getName() != null && !l.getName().isBlank()) {
                    combinedLabelNames.add(l.getName().trim().toLowerCase());
                }
            }
        }

        return TaskDto.builder()
                .id(t.getId())
                .publicId(t.getPublicId())
                .key(taskCode)
                .code(taskCode)
                .columnId(t.getColumnId())
                .workspaceId(t.getWorkspaceId())
                .parentId(t.getParentId())
                .title(t.getTitle())
                .description(t.getDescription())
                .lexoRank(t.getLexoRank())
                .rank(t.getLexoRank())
                .priority(t.getPriority() != null ? t.getPriority() : Priority.MEDIUM)
                .issueType(t.getIssueType() != null ? t.getIssueType() : IssueType.TASK)
                .dueDate(t.getDueDate())
                .storyPoints(t.getStoryPoints())
                .estimatedHours(t.getEstimatedHours())
                .loggedHours(t.getLoggedHours())
                .isOverdue(t.getDueDate() != null && t.getDueDate().isBefore(AppTime.today()))
                .totalChecklistItems(t.getChecklistItems() != null ? t.getChecklistItems().size() : 0)
                .completedChecklistItems(t.getChecklistItems() != null ? (int) t.getChecklistItems().stream().filter(TaskChecklistItem::getIsCompleted).count() : 0)
                .sprintId(t.getSprintId())
                .labels(t.getLabels() != null ? t.getLabels().stream().map(LabelDto::fromEntity).collect(Collectors.toList()) : List.of())
                .labelNames(combinedLabelNames)
                .directions(t.getDirections() != null ? t.getDirections().stream().map(DirectionDto::fromEntity).collect(Collectors.toList()) : List.of())
                .assignees(t.getAssignees() != null ? t.getAssignees().stream().map(UserDto::fromEntity).collect(Collectors.toList()) : List.of())
                .members(memberNames)
                .assignee(UserSummaryDto.fromEntity(firstAssignee))
                .reporter(reporterDto)
                .watchers(t.getWatchers() != null ? t.getWatchers().stream().map(UserDto::fromEntity).collect(Collectors.toList()) : List.of())
                .isArchived(t.getIsArchived())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
