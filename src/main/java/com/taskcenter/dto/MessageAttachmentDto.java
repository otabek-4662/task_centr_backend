package com.taskcenter.dto;

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
public class MessageAttachmentDto {

    @Schema(description = "Attachment ID")
    private String id;

    @Schema(description = "Fayl URL manzili")
    private String fileUrl;

    @Schema(description = "Fayl formati (MIME type)")
    private String fileType;

    @Schema(description = "Fayl hajmi (baytlarda)")
    private Long fileSize;

    @Schema(description = "Rasm uchun thumbnail URL (ixtiyoriy)")
    private String thumbnailUrl;
}
