package com.taskcenter.controller;

import com.jayway.jsonpath.JsonPath;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
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
class TaskActivityControllerTest {

    private static final String SEED_WS = "6a45163133ff7819b28ef909";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ColumnRepository columnRepository;

    @Autowired
    private TaskRepository taskRepository;

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
    @DisplayName("Task o'zgarishi tarixda (Activities) avtomatik qayd etiladi")
    void taskUpdate_recordsActivity() throws Exception {
        String auth = bearer("elshod");
        BoardColumn col = columnRepository.findByWorkspaceIdOrderByOrderAsc(SEED_WS).get(0);

        // 1. Yangi task yaratish
        MvcResult createResult = mvc.perform(post("/api/workspaces/" + SEED_WS + "/tasks")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnId\":\"" + col.getId() + "\",\"title\":\"Original Title\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String taskId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        // 2. Task nomini yangilash
        mvc.perform(put("/api/workspaces/" + SEED_WS + "/tasks/" + taskId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Changed Title\"}"))
                .andExpect(status().isOk());

        // 3. Task tarixini olish (Activities)
        mvc.perform(get("/api/tasks/" + taskId + "/activities")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[?(@.actionType=='TITLE_UPDATED')].oldValue").value("Original Title"))
                .andExpect(jsonPath("$.data.content[?(@.actionType=='TITLE_UPDATED')].newValue").value("Changed Title"))
                .andExpect(jsonPath("$.data.content[?(@.actionType=='TITLE_UPDATED')].userName").value("elshod"));
    }
}
