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
@Schema(
        description = "Shaxsiy (DM) xabar yuborish so'rovi",
        example = "{\"recipientId\": \"uuid-user-456\", \"content\": \"Ertaga uchrashamizmi?\", \"replyToId\": \"msg-123\"}"
)
public class SendDirectMessageRequest {

    @NotBlank(message = "Qabul qiluvchi identifikatori bo'sh bo'lishi mumkin emas")
    @Schema(description = "Xabar qabul qiluvchi user IDsi", example = "uuid-user-456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String recipientId;

    @Size(max = 2000, message = "Xabar matni 2000 belgidan oshmasligi kerak")
    @Schema(description = "Xabar matni", example = "Ertaga uchrashamizmi?", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String content;

    @Schema(description = "Fayl biriktirmalari", example = "[]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private java.util.List<FileUploadResponse> attachments;

    @Schema(description = "Javob beriladigan xabar IDsi (ixtiyoriy)", example = "msg-123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String replyToId;

    @jakarta.validation.constraints.AssertTrue(message = "Xabar matni yoki fayl biriktirilgan bo'lishi shart")
    @Schema(hidden = true)
    public boolean isValidMessage() {
        return (content != null && !content.trim().isEmpty()) || (attachments != null && !attachments.isEmpty());
    }
}
