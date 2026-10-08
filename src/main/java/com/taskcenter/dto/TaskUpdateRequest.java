package com.taskcenter.dto;

import com.taskcenter.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
@Schema(
        description = "Vazifani yangilash so'rovi (barcha maydonlar ixtiyoriy)",
        example = "{\"title\": \"Yangi vazifa nomi\", \"description\": \"Vazifaning yangilangan tafsilotlari\", \"priority\": \"HIGH\", \"issueType\": \"TASK\", \"dueDate\": \"2026-10-20\"}"
)
public class TaskUpdateRequest {
    @Schema(description = "Yangi sarlavha", example = "Yangi vazifa nomi", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @jakarta.validation.constraints.Pattern(regexp = "^.*\\S.*$", message = "title faqat bo'sh joylardan iborat bo'lishi mumkin emas")
    @Size(max = 255, message = "title bo'sh bo'lmasligi va 255 belgidan oshmasligi kerak")
    private String title;

    @Schema(description = "Yangi batafsil tavsif", example = "Vazifaning yangilangan tafsilotlari", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 5000, message = "description 5000 belgidan oshmasligi kerak")
    private String description;

    @Schema(description = "Ko'chiriladigan ustun ID si", example = "col-123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String columnId;

    @Schema(description = "LexoRank yangi tartib qiymati", example = "0|hzzzzz:", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String lexoRank;

    @Schema(description = "Yangi muhimlik darajasi (LOW, MEDIUM, HIGH, URGENT)", example = "HIGH", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Priority priority;

    @Schema(hidden = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String sprintId;

    @Schema(description = "Biriktirilgan foydalanuvchilar ID lari (multiple, to'liq sinxron yangilash)", example = "[\"usr-1\", \"usr-2\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Set<String> assigneeIds;

    @Schema(description = "Yo'nalishlar (Direction) ID lari (multiple, to'liq sinxron yangilash)", example = "[\"dir-1\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Set<String> directionIds;

    @Schema(description = "Teglar (Label) ID lari (multiple, to'liq sinxron yangilash)", example = "[\"lbl-1\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Set<String> labelIds;
}
