package com.taskcenter.dto;

import com.taskcenter.model.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {
    @Schema(description = "Biriktirma identifikatori (UUID)", example = "att-12345")
    private String id;

    @Schema(description = "Tegishli vazifa identifikatori (UUID)", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String taskId;

    @Schema(description = "Yuklagan foydalanuvchi identifikatori", example = "u-12345")
    private String uploaderId;

    @Schema(description = "Yuklagan foydalanuvchi ismi", example = "xusanboy")
    private String uploaderName;

    @Schema(description = "Fayl asl nomi", example = "screenshot.png")
    private String fileName;

    @Schema(description = "Fayl MIME turi", example = "image/png")
    private String fileType;

    @Schema(description = "Fayl hajmi baytlarda", example = "1048576")
    private Long fileSize;

    @Schema(description = "Faylni yuklab olish URL manzili", example = "/api/attachments/att-12345/download")
    private String downloadUrl;

    @Schema(description = "Yuklangan vaqti", example = "2026-10-01T14:30:00")
    private LocalDateTime createdAt;

    public static AttachmentDto fromEntity(Attachment a) {
        String uploaderName = a.getUploader() != null ? a.getUploader().getName() : null;
        return AttachmentDto.builder()
                .id(a.getId())
                .taskId(a.getTaskId())
                .uploaderId(a.getUploaderId())
                .uploaderName(uploaderName)
                .fileName(a.getFileName())
                .fileType(a.getFileType())
                .fileSize(a.getFileSize())
                .downloadUrl("/api/attachments/" + a.getId() + "/download")
                .createdAt(a.getCreatedAt())
                .build();
    }
}
