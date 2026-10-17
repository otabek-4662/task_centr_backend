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
public class AdminWorkspaceDto {
    private String id;
    private String name;
    private String description;
    private String ownerId;
    private String ownerName;
    private int memberCount;
    private int taskCount;
    private LocalDateTime createdAt;
}
