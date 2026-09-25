package com.taskcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionDto {
    private String id;
    private String userId;
    private String userName;
    private String emoji;
    private LocalDateTime createdAt;
}
