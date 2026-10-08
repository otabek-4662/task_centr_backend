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
        description = "Umumiy chatga xabar yuborish so'rovi",
        example = "{\"content\": \"Hammaga salom!\", \"replyToId\": \"msg-12345\"}"
)
public class SendPublicMessageRequest {

    @Size(max = 2000, message = "Xabar matni 2000 belgidan oshmasligi kerak")
    @Schema(description = "Xabar matni", example = "Hammaga salom!", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String content;

    @Schema(description = "Fayl biriktirmalari", example = "[]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private java.util.List<FileUploadResponse> attachments;

    @Schema(description = "Javob beriladigan xabar IDsi (ixtiyoriy)", example = "msg-12345", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String replyToId;

    @jakarta.validation.constraints.AssertTrue(message = "Xabar matni yoki fayl biriktirilgan bo'lishi shart")
    @Schema(hidden = true)
    public boolean isValidMessage() {
        return (content != null && !content.trim().isEmpty()) || (attachments != null && !attachments.isEmpty());
    }
}
