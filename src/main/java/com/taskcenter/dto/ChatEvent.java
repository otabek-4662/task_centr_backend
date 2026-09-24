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
public class ChatEvent<T> {

    @Schema(description = "Event turi", example = "MESSAGE_EDITED")
    private String type;

    @Schema(description = "Event ma'lumotlari")
    private T data;

    public static <T> ChatEvent<T> of(String type, T data) {
        return ChatEvent.<T>builder()
                .type(type)
                .data(data)
                .build();
    }
}
