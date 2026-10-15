package com.taskcenter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddDependencyRequest {

    @NotBlank(message = "Blocker task ID or Public ID is required")
    private String blockerTaskIdOrCode;
}
