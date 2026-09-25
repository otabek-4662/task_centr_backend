package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    @Schema(description = "Yuklangan fayl URL manzili")
    private String fileUrl;

    @Schema(description = "Yuklangan rasmning thumbnail manzili")
    private String thumbnailUrl;
    
    @Schema(description = "Fayl tipi")
    private String fileType;

    @Schema(description = "Fayl hajmi (bytes)")
    private Long fileSize;
}
