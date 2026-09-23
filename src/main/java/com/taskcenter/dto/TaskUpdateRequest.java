package com.taskcenter.dto;

import com.taskcenter.model.IssueType;
import com.taskcenter.model.Priority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskUpdateRequest {
    @Size(max = 255, message = "title 255 belgidan oshmasligi kerak")
    private String title;

    @Size(max = 5000, message = "description 5000 belgidan oshmasligi kerak")
    private String description;

    private String columnId;
    private Integer order;
    private Priority priority;
    private IssueType issueType;
    private LocalDate dueDate;

    @Min(value = 0, message = "storyPoints 0 dan kam bo'lishi mumkin emas")
    @Max(value = 1000, message = "storyPoints 1000 dan oshmasligi kerak")
    private Integer storyPoints;

    private String sprintId;
}
