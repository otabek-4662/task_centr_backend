package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BootstrapResponse {
    @Schema(description = "Joriy foydalanuvchi ma'lumotlari")
    private UserDto currentUser;
    
    @Schema(description = "Foydalanuvchiga tegishli ishchi maydonlar (workspaces)")
    private List<WorkspaceListDto> workspaces;
}
