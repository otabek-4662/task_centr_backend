package com.taskcenter.dto;

import com.taskcenter.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskCreateRequest {
    @NotBlank(message = "columnId bo'sh bo'lishi mumkin emas")
    private String columnId;

    @NotBlank(message = "title bo'sh bo'lishi mumkin emas")
    @Size(max = 255, message = "title 255 belgidan oshmasligi kerak")
    private String title;

    @Size(max = 2000, message = "description 2000 belgidan oshmasligi kerak")
    private String description;

    private Integer order;

    private String assigneeId;

    @FutureOrPresent
    private LocalDateTime dueDate;

    private Priority priority = Priority.MEDIUM;
}
