package com.taskcenter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .addServersItem(new Server().url("http://localhost:8080").description("Local - localhost:8080"))
            .addServersItem(new Server().url("https://schematic-crewmate-repaint.ngrok-free.app").description("Ngrok - production tunnel"));
    }
}