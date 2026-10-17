package com.taskcenter.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBroadcastRequest {
    @NotBlank(message = "Xabarnoma matni bo'sh bo'lishi mumkin emas")
    private String message;

    @Builder.Default
    private boolean silent = false;
}
