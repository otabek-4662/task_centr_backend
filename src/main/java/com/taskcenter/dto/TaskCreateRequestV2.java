package com.taskcenter.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.taskcenter.model.IssueType;
import com.taskcenter.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@Schema(
        description = "Yangi vazifa yaratish so'rovi (v2)",
        example = "{\"columnId\": \"col-12345\", \"title\": \"Swagger UI integratsiyasini ulash\", \"description\": \"Frontendda Swagger UI guruhlarini ko'rsatish\", \"priority\": \"HIGH\", \"issueType\": \"TASK\", \"dueDate\": \"2026-10-15\", \"storyPoints\": 5, \"direction\": [\"Frontend\", \"Backend\"], \"users\": [\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\"]}"
)
public record TaskCreateRequestV2(
        @Schema(
                description = "Vazifa joylashadigan ustun ID si",
                example = "col-12345",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Ustun tanlanishi shart")
        String columnId,

        @Schema(
                description = "Vazifa nomi (maksimal 255 belgi)",
                example = "Swagger UI integratsiyasini ulash",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Vazifa sarlavhasi bo'sh bo'lishi mumkin emas")
        @Size(max = 255, message = "Sarlavha 255 belgidan oshmasligi kerak")
        String title,

        @Schema(
                description = "Vazifa batafsil tavsifi",
                example = "Frontendda Swagger UI guruhlarini ko'rsatish",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 5000, message = "Tavsif 5000 belgidan oshmasligi kerak")
        String description,

        @Schema(
                description = "Muhimlik darajasi (LOW, MEDIUM, HIGH, URGENT)",
                example = "HIGH",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Priority priority,

        @Schema(
                description = "Vazifa turi (TASK, BUG, STORY, EPIC)",
                example = "TASK",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        IssueType issueType,

        @Schema(
                description = "Bajarilish muddati (YYYY-MM-DD)",
                example = "2026-10-15",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dueDate,

        @Schema(
                description = "Story Points (Agile bahosi, 0 yoki undan katta butun son)",
                example = "5",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Min(value = 0, message = "Story points manfiy bo'lishi mumkin emas")
        Integer storyPoints,

        @Schema(
                description = "Yo'nalishlar (Direction) ro'yxati (nomlari yoki ID lari)",
                example = "[\"Frontend\", \"Backend\"]",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        List<String> direction,

        @Schema(
                description = "Biriktirilgan foydalanuvchilar ID lari ro'yxati (ushbu workspace a'zolari)",
                example = "[\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\"]",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        List<String> users
) {}
