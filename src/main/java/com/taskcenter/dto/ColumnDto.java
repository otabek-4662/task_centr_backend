package com.taskcenter.dto;

import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
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
    private String id;
    private String workspaceId;
    private String title;
    private Integer order;
    private Boolean isDefault;
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
                .tasks(taskList)
                .build();
    }
}
