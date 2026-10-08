package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        description = "Vazifaga ijrochilar biriktirish so'rovi",
        example = "{\"userId\": \"a1b2c3d4-e5f6-7890-abcd-ef1234567890\", \"assigneeIds\": [\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\"]}"
)
public class TaskAssignRequest {
    @Schema(description = "Bitta foydalanuvchini biriktirish / olib tashlash uchun userId (toggle uchun)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String userId;

    @Schema(description = "Bir nechta foydalanuvchilar ID lari (to'liq sinxron yangilash uchun)", example = "[\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Set<String> assigneeIds;
}
