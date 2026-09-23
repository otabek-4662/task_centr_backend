package com.taskcenter.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SprintUpdateRequest {
    @Size(max = 100, message = "Sprint nomi 100 belgidan oshmasligi kerak")
    private String name;

    @Size(max = 2000, message = "Sprint maqsadi 2000 belgidan oshmasligi kerak")
    private String goal;

    private LocalDate startDate;
    private LocalDate endDate;
}
