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
public class AdminUserDto {
    private String id;
    private String name;
    private String fullName;
    private String email;
    private String role;
    private Long telegramChatId;
    private boolean telegramLinked;
    private LocalDateTime lastSeenAt;
    private int workspaceCount;
}
