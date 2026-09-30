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
    @Size(min = 1, max = 255, message = "title bo'sh bo'lmasligi va 255 belgidan oshmasligi kerak")
    private String title;

    @Size(max = 5000, message = "description 5000 belgidan oshmasligi kerak")
    private String description;

    private String columnId;
    private String lexoRank;
    private Priority priority;
    private IssueType issueType;
    private LocalDate dueDate;
    private Boolean clearDueDate;

    @Min(value = 0, message = "storyPoints 0 dan kam bo'lishi mumkin emas")
    @Max(value = 1000, message = "storyPoints 1000 dan oshmasligi kerak")
    private Integer storyPoints;

    @Min(value = 0, message = "estimatedHours 0 dan kam bo'lishi mumkin emas")
    private Double estimatedHours;

    @Min(value = 0, message = "loggedHours 0 dan kam bo'lishi mumkin emas")
    private Double loggedHours;

    private String sprintId;
}
