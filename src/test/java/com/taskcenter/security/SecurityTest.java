package com.taskcenter.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityTest {

    @Autowired
    private MockMvc mvc;

    @Value("${jwt.secret}")
    private String secret;

    private String signedWith(String key, Date expiry) {
        return Jwts.builder()
                .setSubject("elshod")
                .setIssuedAt(new Date(System.currentTimeMillis() - 10_000))
                .setExpiration(expiry)
                .signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void protectedEndpoints_withoutToken_return403() throws Exception {
        mvc.perform(get("/api/workspaces")).andExpect(status().isForbidden());
        mvc.perform(get("/api/workspaces/6a45163133ff7819b28ef909/columns"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/workspaces/6a45163133ff7819b28ef909/board"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/me")).andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_expiredToken_returns403() throws Exception {
        String expired = signedWith(secret, new Date(System.currentTimeMillis() - 5_000));

        mvc.perform(get("/api/workspaces").header("Authorization", "Bearer " + expired))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_tamperedToken_returns403() throws Exception {
        String valid = signedWith(secret, new Date(System.currentTimeMillis() + 600_000));
        // Tamper a middle char: flipping the last base64 char can decode to
        // identical bytes (it carries only 2 significant bits) and stay valid.
        int idx = valid.length() / 2;
        char original = valid.charAt(idx);
        char replacement = original == 'a' ? 'Z' : 'a';
        String tampered = valid.substring(0, idx) + replacement + valid.substring(idx + 1);

        mvc.perform(get("/api/workspaces").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_wrongSecret_returns403() throws Exception {
        String foreign = signedWith(
                "completely-different-secret-that-is-also-256-bits-long-xyz",
                new Date(System.currentTimeMillis() + 600_000));

        mvc.perform(get("/api/workspaces").header("Authorization", "Bearer " + foreign))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_algNoneToken_returns403() throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"elshod\"}".getBytes(StandardCharsets.UTF_8));

        mvc.perform(get("/api/workspaces").header("Authorization", "Bearer " + header + "." + payload + "."))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_missingBearerPrefix_returns403() throws Exception {
        String valid = signedWith(secret, new Date(System.currentTimeMillis() + 600_000));

        mvc.perform(get("/api/workspaces").header("Authorization", "Token " + valid))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_sqlInjectionTreatedAsLiteral_not500() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"' OR '1'='1\",\"password\":\"' OR '1'='1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_sqlInjectionName_storedLiterally() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"injected' OR '1'='1\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void swaggerUi_isPublic() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().is2xxSuccessful());
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void cors_preflight_allowsConfiguredOrigins() throws Exception {
        mvc.perform(options("/api/workspaces")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void actuatorHealth_isPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").exists());
    }

    private String registerAndGetToken(String name) throws Exception {
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.data.token");
    }

    @Test
    void telegramLinking_meShowsFlag_tokenHasShape_unlinkAllowedForUser() throws Exception {
        String jwt = registerAndGetToken("tglinkuser");

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.telegramLinked").value(false))
                .andExpect(jsonPath("$.data.telegramChatId").doesNotExist());

        mvc.perform(get("/api/users/me/telegram-link-token").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.expiresAt").exists());

        mvc.perform(delete("/api/users/me/telegram").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void telegramUnlink_withoutToken_returns403() throws Exception {
        mvc.perform(delete("/api/users/me/telegram")).andExpect(status().isForbidden());
    }

    @Test
    void deleteOtherUsers_stillAdminOnly() throws Exception {
        String jwt = registerAndGetToken("tgplainuser");
        mvc.perform(delete("/api/users/some-other-id").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void appEndpoint_allowsFraming_forTelegramMiniApp() throws Exception {
        mvc.perform(get("/app/index.html"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Frame-Options"));
    }

    @Test
    void apiEndpoint_deniesFraming_byDefault() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void adminEndpoint_userRole_rejected() throws Exception {
        String token = signedWith(secret, new Date(System.currentTimeMillis() + 600_000));
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void createWorkspace_oversizedTitle_rejected() throws Exception {
        String token = signedWith(secret, new Date(System.currentTimeMillis() + 600_000));
        String longTitle = "T".repeat(300);
        mvc.perform(post("/api/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + longTitle + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWorkspace_invalidBgColor_rejected() throws Exception {
        String token = signedWith(secret, new Date(System.currentTimeMillis() + 600_000));
        mvc.perform(post("/api/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Ws\",\"bgColor\":\"not-a-color\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void taskDirect_noToken_returns403() throws Exception {
        mvc.perform(get("/api/tasks/some-id"))
                .andExpect(status().isForbidden());
    }

    @Test
    void taskDirect_nonMember_returns404() throws Exception {
        String token = signedWith(secret, new Date(System.currentTimeMillis() + 600_000));
        mvc.perform(get("/api/tasks/some-id")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()); // doesn't leak
    }

    @Test
    void telegramMe_noToken_returns403() throws Exception {
        mvc.perform(post("/api/users/me/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"fake\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void appEndpoint_headers_arePresent() throws Exception {
        mvc.perform(get("/app/index.html"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("frame-ancestors https://web.telegram.org")))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().doesNotExist("X-Frame-Options"));
    }
}
