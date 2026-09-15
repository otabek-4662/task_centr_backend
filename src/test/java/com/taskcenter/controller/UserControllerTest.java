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
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    private String bearerForFreshUser() throws Exception {
        String name = "stats-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"password\":\"password123\"}"))
                .andExpect(status().is2xxSuccessful());

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + JsonPath.read(login.getResponse().getContentAsString(), "$.data.token");
    }

    private String myId(String auth) throws Exception {
        MvcResult me = mvc.perform(get("/api/me")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(me.getResponse().getContentAsString(), "$.data.id");
    }

    @Test
    void me_stats_returnsZeroCountsForFreshUser() throws Exception {
        String auth = bearerForFreshUser();

        mvc.perform(get("/api/me/stats")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskCount").value(0))
                .andExpect(jsonPath("$.data.workspaceCount").value(0));
    }

    @Test
    void me_stats_countsGrowAfterWorkspaceAndAssignedTask() throws Exception {
        String auth = bearerForFreshUser();
        String userId = myId(auth);

        MvcResult ws = mvc.perform(post("/api/workspaces")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Stats WS\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String wsId = JsonPath.read(ws.getResponse().getContentAsString(), "$.data.id");

        MvcResult col = mvc.perform(post("/api/workspaces/" + wsId + "/columns")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Col\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String colId = JsonPath.read(col.getResponse().getContentAsString(), "$.data.id");

        mvc.perform(post("/api/workspaces/" + wsId + "/tasks")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"columnId\":\"" + colId + "\",\"assigneeId\":\"" + userId + "\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/me/stats")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskCount").value(1))
                .andExpect(jsonPath("$.data.workspaceCount").value(1));
    }

    @Test
    void me_stats_unauthenticated_returnsError() throws Exception {
        mvc.perform(get("/api/me/stats"))
                .andExpect(status().is4xxClientError());
    }
}
