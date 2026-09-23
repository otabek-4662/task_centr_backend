package com.taskcenter.dto;

import com.taskcenter.model.Attachment;
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
    private String id;
    private String taskId;
    private String uploaderId;
    private String uploaderName;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String downloadUrl;
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
