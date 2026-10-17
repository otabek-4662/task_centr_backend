package com.taskcenter.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSystemJobDto {
    private String jobKey;
    private String name;
    private String description;
    private String status;
    private LocalDateTime lastRunAt;
}
