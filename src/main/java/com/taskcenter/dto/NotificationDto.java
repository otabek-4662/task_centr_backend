package com.taskcenter.dto;

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
public class NotificationDto {

    private String id;
    private String title;
    private String message;
    private boolean read;
    private String type;
    private String referenceId;
    private LocalDateTime createdAt;
}
