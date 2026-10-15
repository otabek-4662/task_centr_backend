package com.taskcenter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartTimerRequest {
    private String taskId;
    @Builder.Default
    private Integer targetMinutes = 25;
}
