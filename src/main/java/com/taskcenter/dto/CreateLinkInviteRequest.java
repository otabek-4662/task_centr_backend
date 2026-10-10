package com.taskcenter.dto;

import com.taskcenter.model.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Havola orqali taklif qilish so'rovi (Trello/Jira mantiqida)")
public class CreateLinkInviteRequest {

    @Schema(description = "Beriladigan rol (ADMIN, MEMBER, VIEWER). OWNER bo'lishi mumkin emas.", example = "MEMBER")
    @Builder.Default
    private WorkspaceRole role = WorkspaceRole.MEMBER;

    @Schema(description = "Maksimal foydalanish soni (bo'sh yoki 0 bo'lsa cheksiz)", example = "25")
    @Min(value = 0, message = "maxUses 0 yoki musbat son bo'lishi kerak")
    @Max(value = 1000, message = "maxUses 1000 tadan oshmasligi kerak")
    private Integer maxUses;

    @Schema(description = "Amal qilish muddati kunlarda (standart 7 kun)", example = "7")
    @Min(value = 1, message = "Muddati kamida 1 kun bo'lishi kerak")
    @Max(value = 90, message = "Muddati 90 kundan oshmasligi kerak")
    @Builder.Default
    private Integer durationDays = 7;
}
