package com.taskcenter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.GitHubPushEventDto;
import com.taskcenter.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/webhooks")
@Tag(name = "Webhooks", description = "Tashqi tizimlar bilan integratsiya (GitHub va boshqalar)")
public class WebhookController {

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    public WebhookController(WebhookService webhookService, ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/github")
    @Operation(summary = "GitHub Push eventni qabul qilish (X-Hub-Signature-256 imzosi majburiy)")
    public ResponseEntity<ApiResponse<String>> handleGitHubPush(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType) throws IOException {

        // Imzo xom baytlar ustidan hisoblanadi, shuning uchun JSON ni tekshiruvdan KEYIN o'qiymiz
        webhookService.verifySignature(rawBody, signature);

        // Biz faqat 'push' hodisalarni qayta ishlaymiz
        if ("push".equalsIgnoreCase(eventType)) {
            GitHubPushEventDto payload = objectMapper.readValue(rawBody, GitHubPushEventDto.class);
            webhookService.processGitHubPushEvent(payload);
        }
        // Webhook serverlarga (GitHub'ga) odatda 200 OK qaytarish kerak
        return ResponseEntity.ok(ApiResponse.success("Webhook qabul qilindi", null));
    }
}
