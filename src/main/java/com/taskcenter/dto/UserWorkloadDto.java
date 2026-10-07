package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWorkloadDto {
    @Schema(description = "Foydalanuvchi identifikatori (UUID)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String userId;

    @Schema(description = "Foydalanuvchi login nomi", example = "xusanboy")
    private String userName;

    @Schema(description = "Foydalanuvchi to'liq ismi", example = "Xusanboy Developer")
    private String userFullName;
    
    // Foydalanuvchiga biriktirilgan jami vazifalar
    @Schema(description = "Biriktirilgan jami vazifalar soni", example = "10")
    private long totalAssignedTasks;
    
    // Shundan qanchasi Done (Bajarilgan) ustunida
    @Schema(description = "Bajarilgan vazifalar soni", example = "6")
    private long completedTasks;
    
    // Faol vazifalar (To Do + In Progress)
    @Schema(description = "Faol (bajarilmagan) vazifalar soni", example = "4")
    private long activeTasks;
}
