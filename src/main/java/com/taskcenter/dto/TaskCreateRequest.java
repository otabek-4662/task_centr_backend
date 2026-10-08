package com.taskcenter.dto;

import com.taskcenter.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
@Schema(
        description = "Yangi vazifa yaratish so'rovi",
        example = "{\"columnId\": \"col-12345\", \"title\": \"Swagger UI integratsiyasini ulash\", \"description\": \"Frontendda Swagger UI guruhlarini ko'rsatish\", \"priority\": \"HIGH\", \"issueType\": \"TASK\", \"dueDate\": \"2026-10-15\", \"storyPoints\": 5}"
)
public class TaskCreateRequest {
    @Schema(description = "Vazifa joylashadigan ustun ID si", example = "col-12345", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Ustun tanlanishi shart")
    private String columnId;

    @Schema(description = "Vazifa nomi", example = "Swagger UI integratsiyasini ulash", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Vazifa (Bosh og'riq) sarlavhasi bo'sh bo'lishi mumkin emas")
    @Size(max = 255, message = "Sarlavha 255 belgidan oshmasligi kerak")
    private String title;

    @Schema(description = "Vazifa batafsil tavsifi", example = "Frontendda Swagger UI guruhlarini ko'rsatish", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 5000, message = "Tavsif 5000 belgidan oshmasligi kerak")
    private String description;

    @Schema(description = "Lexorank saralash qiymati", example = "0|hzzzzz:", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String lexoRank;

    @Schema(description = "Muhimlik darajasi (LOW, MEDIUM, HIGH, URGENT)", example = "HIGH", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Priority priority;

    @Schema(hidden = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String sprintId;

    @Schema(description = "Biriktirilgan foydalanuvchilar ID lari (multiple)", example = "[\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Set<String> assigneeIds;

    @Schema(description = "Yo'nalishlar (Direction) ID lari (multiple, masalan: Frontend, Backend)", example = "[\"dir-1\", \"dir-2\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Set<String> directionIds;

    @Schema(description = "Teglar (Label) ID lari (multiple)", example = "[\"label-123\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Set<String> labelIds;
}
