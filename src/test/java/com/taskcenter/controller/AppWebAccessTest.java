package com.taskcenter.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:app-web-access;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "telegram.bot.enabled=false",
        "telegram.bot.token=123456:app-web-access-test-token"
})
@ActiveProfiles({"test", "prod"})
class AppWebAccessTest {

    @Autowired
    private TestRestTemplate http;

    @BeforeEach
    void useNonRedirectingHttpClient() {
        http.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void rootRedirectsToApp() {
        ResponseEntity<String> response = http.getForEntity("/", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().getPath()).isEqualTo("/app/");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/app", "/app/", "/app/index.html"})
    void appHtmlIsPublic(String path) {
        ResponseEntity<String> response = http.getForEntity(path, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML)).isTrue();
        assertThat(response.getBody()).contains("Choylashamiz");
        assertThat(response.getHeaders().getFirst("Content-Security-Policy")).contains("frame-ancestors 'self'");
        assertThat(response.getHeaders().getFirst("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/app/"})
    void frontendAssetsSupportAnonymousGetAndHead(String prefix) {
        assertAsset(prefix + "style.css?v=46", "text/css");
        assertAsset(prefix + "app.js?v=47", "text/javascript", "application/javascript");
        assertAsset(prefix + "games.js?v=4", "text/javascript", "application/javascript");
        assertAsset(prefix + "ui-bind.js?v=54", "text/javascript", "application/javascript");
        assertAsset(prefix + "piyola-3d.png", "image/png");
        assertAsset(prefix + "choylashamiz-logo.png", "image/png");
    }

    private void assertAsset(String path, String... mediaTypes) {
        ResponseEntity<byte[]> get = http.getForEntity(path, byte[].class);
        ResponseEntity<byte[]> head = http.exchange(path, HttpMethod.HEAD, HttpEntity.EMPTY, byte[].class);
        for (ResponseEntity<byte[]> response : List.of(get, head)) {
            assertThat(response.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
            MediaType contentType = response.getHeaders().getContentType();
            assertThat(contentType).as(path).isNotNull();
            assertThat(contentType.getType() + "/" + contentType.getSubtype()).as(path).isIn(mediaTypes);
            assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).as(path).isEqualTo("nosniff");
        }
        assertThat(get.getBody()).as(path).isNotEmpty();
        assertThat(head.getBody()).as(path).isNullOrEmpty();
    }

    @Test
    void invalidTelegramCredentialsAreIgnoredForPublicAssets() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Telegram-Init-Data", "hash=invalid");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        for (String path : new String[]{"/style.css?v=46", "/app/style.css?v=46"}) {
            assertThat(http.exchange(path, HttpMethod.GET, request, byte[].class).getStatusCode())
                    .as(path).isEqualTo(HttpStatus.OK);
        }
        ResponseEntity<String> api = http.exchange("/api/me", HttpMethod.GET, request, String.class);
        assertThat(api.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(api.getBody()).contains("Invalid or expired Telegram WebApp initData");
    }

    @Test
    void protectedPathsAndAssetWritesRequireAuthorization() {
        assertThat(http.getForEntity("/api/workspaces", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.getForEntity("/api/workspaces/style.css", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        for (String path : new String[]{"/style.css", "/app/style.css"}) {
            assertThat(http.postForEntity(path, null, String.class).getStatusCode())
                    .as(path).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        }
    }
}
