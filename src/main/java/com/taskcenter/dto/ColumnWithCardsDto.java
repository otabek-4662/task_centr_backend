package com.taskcenter.dto;

import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import lombok.*;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnWithCardsDto {
    private String id;
    private String title;
    private Integer order;
    private List<TaskCardDto> cards;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskCardDto {
        private String id;
        private String publicId;
        private String title;
        private Integer order;

        public static TaskCardDto fromEntity(Task t) {
            return TaskCardDto.builder()
                    .id(t.getId())
                    .publicId(t.getPublicId())
                    .title(t.getTitle())
                    .order(t.getOrder())
                    .build();
        }
    }

    public static ColumnWithCardsDto fromEntity(BoardColumn c, List<Task> tasks) {
        return ColumnWithCardsDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .order(c.getOrder())
                .cards(tasks.stream().map(TaskCardDto::fromEntity).collect(Collectors.toList()))
                .build();
    }
}
