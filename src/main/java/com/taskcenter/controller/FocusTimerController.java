package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.FocusTimerDto;
import com.taskcenter.dto.StartTimerRequest;
import com.taskcenter.model.User;
import com.taskcenter.service.FocusTimerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timers")
@Tag(name = "Focus Timers", description = "Pomodoro / Fokus taymerlari")
@SecurityRequirement(name = "bearerAuth")
public class FocusTimerController {

    private final FocusTimerService focusTimerService;

    public FocusTimerController(FocusTimerService focusTimerService) {
        this.focusTimerService = focusTimerService;
    }

    @PostMapping("/start")
    @Operation(summary = "Pomodoro taymerini boshlash")
    public ResponseEntity<ApiResponse<FocusTimerDto>> startTimer(
            @RequestBody(required = false) StartTimerRequest request,
            @AuthenticationPrincipal User currentUser) {
        String taskId = request != null ? request.getTaskId() : null;
        Integer targetMinutes = request != null ? request.getTargetMinutes() : 25;
        FocusTimerDto dto = focusTimerService.startTimer(taskId, targetMinutes, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Taymer boshlandi", dto));
    }

    @PostMapping("/stop")
    @Operation(summary = "Faol Pomodoro taymerini to'xtatish va ishlangan vaqtni yozish")
    public ResponseEntity<ApiResponse<FocusTimerDto>> stopTimer(@AuthenticationPrincipal User currentUser) {
        FocusTimerDto dto = focusTimerService.stopTimer(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Taymer to'xtatildi", dto));
    }

    @GetMapping("/active")
    @Operation(summary = "Joriy foydalanuvchining faol taymerini olish")
    public ResponseEntity<ApiResponse<FocusTimerDto>> getActiveTimer(@AuthenticationPrincipal User currentUser) {
        FocusTimerDto dto = focusTimerService.getActiveTimer(currentUser).orElse(null);
        return ResponseEntity.ok(ApiResponse.success("Faol taymer holati", dto));
    }
}
