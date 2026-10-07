package com.taskcenter.dto;

import com.taskcenter.model.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    @Schema(description = "Izoh identifikatori (UUID)", example = "com-12345")
    private String id;

    @Schema(description = "Vazifa identifikatori (UUID)", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String taskId;

    @Schema(description = "Muallif identifikatori", example = "u-12345")
    private String authorId;

    @Schema(description = "Muallif login nomi", example = "xusanboy")
    private String authorName;

    @Schema(description = "Muallif to'liq ismi", example = "Xusanboy Developer")
    private String authorFullName;

    @Schema(description = "Izoh matni", example = "Ushbu vazifani bugun test qilib ko'ramiz")
    private String content;

    @Schema(description = "Yaratilgan vaqti", example = "2026-10-01T11:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Oxirgi o'zgartirilgan vaqti", example = "2026-10-01T11:05:00")
    private LocalDateTime updatedAt;

    public static CommentDto fromEntity(Comment c) {
        String authorName = c.getAuthor() != null ? c.getAuthor().getName() : null;
        String authorFullName = c.getAuthor() != null ? c.getAuthor().getFullName() : null;
        return CommentDto.builder()
                .id(c.getId())
                .taskId(c.getTaskId())
                .authorId(c.getAuthorId())
                .authorName(authorName)
                .authorFullName(authorFullName != null ? authorFullName : authorName)
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
