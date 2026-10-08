package com.taskcenter.dto;

import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardInitColumnDto {
    @Schema(description = "Ustun identifikatori (UUID)", example = "col-12345")
    private String id;

    @Schema(description = "Ustun nomi", example = "To Do")
    private String title;

    @Schema(description = "Ustun tartib indeksi", example = "0")
    private Integer order;

    @Schema(description = "Ustundagi vazifa kartalari ro'yxati (yengillashtirilgan)")
    private List<TaskCardDto> cards;

    public static BoardInitColumnDto fromEntity(BoardColumn c, List<Task> tasks) {
        return BoardInitColumnDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .order(c.getOrder())
                .cards(tasks.stream().map(TaskCardDto::fromEntity).collect(Collectors.toList()))
                .build();
    }
}
