package com.taskcenter.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    private String uniqueName() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private String register(String name, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    @Test
    void register_returnsToken() throws Exception {
        String name = uniqueName();
        register(name, "password123");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.name").value(name));
    }

    @Test
    void register_duplicateName_returns409() throws Exception {
        String name = uniqueName();
        register(name, "password123");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + uniqueName() + "\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_blankName_returns400() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        String name = uniqueName();
        register(name, "password123");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_unknownUser_returns401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ghost-" + uniqueName() + "\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withToken_returnsUser() throws Exception {
        String name = uniqueName();
        String token = register(name, "password123");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(name));
    }

    @Test
    void me_withoutToken_returns403() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void me_withGarbageToken_returns403() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer garbage.token.here"))
                .andExpect(status().isForbidden());
    }

    private String registerAndGetRefreshToken(String name, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.refreshToken");
    }

    @Test
    void refresh_success_rotatesToken() throws Exception {
        String name = uniqueName();
        String oldRefreshToken = registerAndGetRefreshToken(name, "password123");

        MvcResult result = mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        String newRefreshToken = JsonPath.read(result.getResponse().getContentAsString(), "$.data.refreshToken");
        org.junit.jupiter.api.Assertions.assertNotEquals(oldRefreshToken, newRefreshToken);
    }

    @Test
    void refresh_tokenReuseDetection_invalidatesAllSessions() throws Exception {
        String name = uniqueName();
        String oldRefreshToken = registerAndGetRefreshToken(name, "password123");

        // 1-marta refresh qilish - muvaffaqiyatli, yangi token beriladi
        MvcResult result = mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String newRefreshToken = JsonPath.read(result.getResponse().getContentAsString(), "$.data.refreshToken");

        // 2-marta o'sha bekor qilingan eski tokenni qayta yuborish (Token Reuse Attack!)
        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefreshToken + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // Natijada barcha sessiyalar bekor qilingan: yangi token ham o'chirilgan bo'lishi kerak
        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + newRefreshToken + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void logout_revokesToken() throws Exception {
        String name = uniqueName();
        String refreshToken = registerAndGetRefreshToken(name, "password123");

        mvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Bekor qilingan token bilan refresh qilish urinishi
        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isForbidden());
    }
}
