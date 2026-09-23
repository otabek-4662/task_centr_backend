package com.taskcenter.dto;

import com.taskcenter.model.Comment;
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
    private String id;
    private String taskId;
    private String authorId;
    private String authorName;
    private String authorFullName;
    private String content;
    private LocalDateTime createdAt;
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
