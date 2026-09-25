package com.taskcenter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ToggleReactionRequest {
    @NotBlank
    private String emoji;
}
