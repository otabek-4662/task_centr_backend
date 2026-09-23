package com.taskcenter.dto;

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
public class CommentUpdateRequest {

    @NotBlank(message = "Izoh matni bo'sh bo'lishi mumkin emas")
    @Size(max = 5000, message = "Izoh matni 5000 belgidan oshmasligi kerak")
    private String content;
}
