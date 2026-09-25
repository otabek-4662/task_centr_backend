package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.GitHubPushEventDto;
import com.taskcenter.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@Tag(name = "Webhooks", description = "Tashqi tizimlar bilan integratsiya (GitHub va boshqalar)")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/github")
    @Operation(summary = "GitHub Push eventni qabul qilish")
    public ResponseEntity<ApiResponse<String>> handleGitHubPush(
            @RequestBody GitHubPushEventDto payload,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType) {
        
        // Biz faqat 'push' hodisalarni qayta ishlaymiz
        if ("push".equalsIgnoreCase(eventType)) {
            webhookService.processGitHubPushEvent(payload);
        }

        // Webhook serverlarga (GitHub'ga) odatda 200 OK qaytarish kerak
        return ResponseEntity.ok(ApiResponse.success("Webhook qabul qilindi", null));
    }
}
