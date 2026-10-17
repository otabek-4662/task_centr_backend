package com.taskcenter.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBroadcastResultDto {
    private int totalRecipients;
    private int successfulDeliveries;
    private int failedDeliveries;
    private long durationMs;
}
