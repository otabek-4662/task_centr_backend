package com.taskcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWorkloadDto {
    private String userId;
    private String userName;
    private String userFullName;
    
    // Foydalanuvchiga biriktirilgan jami vazifalar
    private long totalAssignedTasks;
    
    // Shundan qanchasi Done (Bajarilgan) ustunida
    private long completedTasks;
    
    // Faol vazifalar (To Do + In Progress)
    private long activeTasks;
}
