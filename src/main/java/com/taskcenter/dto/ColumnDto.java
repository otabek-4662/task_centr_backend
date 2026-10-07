package com.taskcenter.dto;

import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDto {
    @Schema(description = "Ustun identifikatori (UUID)", example = "col-12345")
    private String id;

    @Schema(description = "Ishchi maydon identifikatori (UUID)", example = "ws-12345")
    private String workspaceId;

    @Schema(description = "Ustun nomi", example = "Bajarilmoqda")
    private String title;

    @Schema(description = "Ustunning doskadagi tartib indeksi", example = "1")
    private Integer order;

    @Schema(description = "Standart (boshlang'ich) ustunmi", example = "true")
    private Boolean isDefault;

    @Schema(description = "Tugallanganlik (Done) ustunimi", example = "false")
    private Boolean isDone;

    @Schema(description = "Ustundagi vazifalar ro'yxati")
    @Builder.Default
    private List<TaskDto> tasks = new ArrayList<>();

    public static ColumnDto fromEntity(BoardColumn c) {
        List<TaskDto> taskList = c.getTasks() != null
                ? c.getTasks().stream()
                    .sorted(Comparator.comparing(Task::getLexoRank, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(TaskDto::fromEntity)
                    .collect(Collectors.toList())
                : new ArrayList<>();

        return ColumnDto.builder()
                .id(c.getId())
                .workspaceId(c.getWorkspaceId())
                .title(c.getTitle())
                .order(c.getOrder())
                .isDefault(c.getIsDefault())
                .isDone(c.getIsDone())
                .tasks(taskList)
                .build();
    }
}
