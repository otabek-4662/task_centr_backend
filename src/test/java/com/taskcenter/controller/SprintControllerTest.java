package com.taskcenter.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SprintControllerTest {

    private static final String SEED_WS = "6a45163133ff7819b28ef909";

    @Autowired
    private MockMvc mvc;

    private String tokenFor(String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private String bearer(String name) throws Exception {
        return "Bearer " + tokenFor(name);
    }

    @Test
    @DisplayName("Sprint yaratish, boshlash, olish va yakunlash to'liq integratsiya testi")
    void sprintLifecycle_success() throws Exception {
        String token = bearer("xusan");

        // 1. Yangi sprint yaratish
        MvcResult createResult = mvc.perform(post("/api/workspaces/" + SEED_WS + "/sprints")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sprint 101\",\"goal\":\"Finish Agile features\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Sprint 101"))
                .andExpect(jsonPath("$.data.status").value("FUTURE"))
                .andReturn();

        String sprintId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        // 2. Sprintlar ro'yxatini olish
        mvc.perform(get("/api/workspaces/" + SEED_WS + "/sprints")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(sprintId));

        // 3. Sprintni boshlash (ACTIVE)
        mvc.perform(post("/api/sprints/" + sprintId + "/start")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 4. Sprintni yakunlash (CLOSED)
        mvc.perform(post("/api/sprints/" + sprintId + "/complete")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        // 5. Backlog ro'yxatini olish
        mvc.perform(get("/api/workspaces/" + SEED_WS + "/backlog")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tasks").isArray());

        // 5b. Doskani sprintId bo'yicha filterlab olish
        mvc.perform(get("/api/workspaces/" + SEED_WS + "/board?sprintId=" + sprintId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 6. Sprintni o'chirish
        mvc.perform(delete("/api/sprints/" + sprintId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
