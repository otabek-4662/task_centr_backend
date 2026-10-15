package com.taskcenter.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FocusTimerDto {
    private String id;
    private String taskId;
    private String taskPublicId;
    private String taskTitle;
    private Integer targetMinutes;
    private Long elapsedSeconds;
    private Double loggedHoursAdded;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Boolean isActive;
}
