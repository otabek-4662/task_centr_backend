package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendPublicMessageRequest {

    @Size(max = 2000, message = "Xabar matni 2000 belgidan oshmasligi kerak")
    @Schema(description = "Xabar matni", example = "Hammaga salom!")
    private String content;

    @Schema(description = "Fayl biriktirmalari")
    private java.util.List<FileUploadResponse> attachments;

    @Schema(description = "Javob beriladigan xabar IDsi (ixtiyoriy)")
    private String replyToId;
}
