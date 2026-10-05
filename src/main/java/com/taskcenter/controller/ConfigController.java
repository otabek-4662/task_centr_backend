package com.taskcenter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Config", description = "Public configuration endpoints")
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.miniapp.base-url}")
    private String miniAppUrl;

    @Operation(summary = "Get public config")
    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> getPublicConfig() {
        return ResponseEntity.ok(Map.of(
                "botUsername", botUsername,
                "miniAppUrl", miniAppUrl
        ));
    }
}
