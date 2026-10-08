package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(
        description = "Yangi ishchi maydon (Workspace) yaratish so'rovi",
        example = "{\"title\": \"Frontend Jamoasi\", \"bgColor\": \"#1a1b41\", \"description\": \"Asosiy frontend loyihalari uchun doska\", \"initDefaultColumns\": true}"
)
public class WorkspaceCreateRequest {
    @Schema(description = "Ishchi maydon nomi", example = "Frontend Jamoasi", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Loyiha (G'alva) nomi bo'sh bo'lishi mumkin emas")
    @Size(max = 255, message = "Loyiha (G'alva) nomi 255 belgidan oshmasligi kerak")
    private String title;

    @Schema(description = "Fon rangi (hex, rgba yoki gradient)", example = "#1a1b41", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Pattern(regexp = "^(#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})|linear-gradient\\(.+\\)|rgba?\\(.+\\))$", 
             message = "bgColor hex (#1a1b41) yoki gradient (linear-gradient(...)) formatida bo'lishi kerak")
    @Size(max = 255, message = "bgColor 255 belgidan oshmasligi kerak")
    private String bgColor;

    @Schema(description = "Qisqacha tavsif", example = "Asosiy frontend loyihalari uchun doska", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 5000, message = "Tavsif 5000 belgidan oshmasligi kerak")
    private String description;

    @Schema(description = "Boshlang'ich default ustunlar (To Do, In Progress, Done) yaratilsinmi?", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean initDefaultColumns;
}
