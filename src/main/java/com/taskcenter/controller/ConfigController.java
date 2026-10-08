package com.taskcenter.controller;

import com.taskcenter.dto.PublicConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Config", description = "Public configuration endpoints")
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.miniapp.base-url}")
    private String miniAppUrl;

    @Operation(
            operationId = "getPublicSystemConfig",
            summary = "Get public config",
            description = "Frontend uchun kerakli ommaviy konfiguratsiya ma'lumotlarini (Telegram bot username, MiniApp URL) qaytaradi."
    )
    @GetMapping("/public")
    public ResponseEntity<PublicConfigResponse> getPublicConfig() {
        return ResponseEntity.ok(new PublicConfigResponse(botUsername, miniAppUrl));
    }
}
