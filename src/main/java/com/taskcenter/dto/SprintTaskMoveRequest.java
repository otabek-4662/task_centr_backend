package com.taskcenter.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SprintTaskMoveRequest {
    @NotEmpty(message = "Task ID lar ro'yxati bo'sh bo'lishi mumkin emas")
    private List<String> taskIds;
}
