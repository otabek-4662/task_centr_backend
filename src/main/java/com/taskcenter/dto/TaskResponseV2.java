package com.taskcenter.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.taskcenter.model.IssueType;
import com.taskcenter.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(
        description = "Vazifa yaratish javobi (v2 toza DTO)",
        example = "{\"id\": \"f47ac10b-58cc-4372-a567-0e02b2c3d479\", \"publicId\": \"PROJ-12\", \"columnId\": \"col-12345\", \"title\": \"Swagger UI integratsiyasini ulash\", \"description\": \"Frontendda Swagger UI guruhlarini ko'rsatish\", \"priority\": \"HIGH\", \"issueType\": \"TASK\", \"dueDate\": \"2026-10-15\", \"storyPoints\": 5, \"direction\": [\"Frontend\", \"Backend\"], \"users\": [{\"id\": \"a1b2c3d4-e5f6-7890-abcd-ef1234567890\", \"name\": \"xusanboy\", \"fullName\": \"Xusanboy Developer\", \"email\": \"xusanboy@example.com\", \"role\": \"MEMBER\", \"roleName\": \"Qora ishchi\"}], \"createdAt\": \"2026-10-09T18:00:00\"}"
)
public record TaskResponseV2(
        @Schema(description = "Vazifaning unikal identifikatori (UUID)", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        String id,

        @Schema(description = "Qisqa inson o'qiy oladigan identifikator", example = "PROJ-12")
        String publicId,

        @Schema(description = "Ustun identifikatori", example = "col-12345")
        String columnId,

        @Schema(description = "Vazifa sarlavhasi", example = "Swagger UI integratsiyasini ulash")
        String title,

        @Schema(description = "Vazifa batafsil tavsifi", example = "Frontendda Swagger UI guruhlarini ko'rsatish")
        String description,

        @Schema(description = "Muhimlik darajasi (LOW, MEDIUM, HIGH, URGENT)", example = "HIGH")
        Priority priority,

        @Schema(description = "Vazifa turi (TASK, BUG, STORY, EPIC)", example = "TASK")
        IssueType issueType,

        @Schema(description = "Bajarilish muddati (YYYY-MM-DD)", example = "2026-10-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dueDate,

        @Schema(description = "Story Points (Agile bahosi)", example = "5")
        Integer storyPoints,

        @Schema(description = "Vazifaga biriktirilgan yo'nalishlar nomlari ro'yxati", example = "[\"Frontend\", \"Backend\"]")
        List<String> direction,

        @Schema(description = "Vazifaga biriktirilgan ijrochilar ro'yxati (ushbu workspace dagi rollari bilan)")
        List<TaskUserDto> users,

        @Schema(description = "Yaratilgan vaqt (ISO-8601)", example = "2026-10-09T18:00:00")
        LocalDateTime createdAt
) {}
