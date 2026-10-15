package com.taskcenter.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDependenciesDto {
    private boolean blocked;

    @Builder.Default
    private List<DependencyItemDto> blockedBy = new ArrayList<>();

    @Builder.Default
    private List<DependencyItemDto> blocking = new ArrayList<>();
}
