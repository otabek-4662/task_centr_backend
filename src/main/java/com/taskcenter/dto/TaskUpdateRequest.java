package com.taskcenter.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.taskcenter.model.IssueType;
import com.taskcenter.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
@Schema(
        description = "Vazifani yangilash so'rovi (barcha maydonlar ixtiyoriy)",
        example = "{\"title\": \"Yangi vazifa nomi\", \"description\": \"Vazifaning yangilangan tafsilotlari\", \"priority\": \"HIGH\", \"issueType\": \"TASK\", \"dueDate\": \"2026-10-20\", \"storyPoints\": 5, \"direction\": [\"Frontend\", \"Backend\"], \"users\": [\"usr-12345\"]}"
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

    @Schema(description = "Vazifa turi (TASK, BUG, STORY, EPIC)", example = "TASK", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private IssueType issueType;

    @Schema(description = "Bajarilish muddati (YYYY-MM-DD)", example = "2026-10-20", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate dueDate;

    @Schema(description = "Story Points (Agile bahosi)", example = "5", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer storyPoints;

    @Schema(hidden = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String sprintId;

    @Schema(description = "Biriktirilgan foydalanuvchilar ID lari (multiple, to'liq sinxron yangilash)", example = "[\"usr-1\", \"usr-2\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Set<String> assigneeIds;

    @Schema(description = "Biriktirilgan foydalanuvchilar (odamlar) ID, username yoki email lari (multiple)", example = "[\"usr-12345\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonAlias({"user", "assignees"})
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private Set<String> users;

    @Schema(description = "Yo'nalishlar (Direction) ID lari (multiple, to'liq sinxron yangilash)", example = "[\"dir-1\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Set<String> directionIds;

    @Schema(description = "Yo'nalishlar (Direction) nomlari yoki ID lari (multiple, masalan: [\"Frontend\", \"Backend\"])", example = "[\"Frontend\", \"Backend\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonAlias({"directions"})
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private Set<String> direction;

    @Schema(description = "Teglar (Label) ID lari (multiple, to'liq sinxron yangilash)", example = "[\"lbl-1\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Set<String> labelIds;
}
