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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkspaceControllerTest {

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

    private String uniqueTitle() {
        return "WS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @Test
    void list_memberSeesSeedWorkspace() throws Exception {
        mvc.perform(get("/api/workspaces").header("Authorization", bearer("elshod")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + SEED_WS + "')]").isArray());
    }

    @Test
    void list_ownerSeesSeedWorkspace() throws Exception {
        mvc.perform(get("/api/workspaces").header("Authorization", bearer("xusan")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + SEED_WS + "')]").isArray());
    }

    @Test
    void list_withoutToken_returns403() throws Exception {
        mvc.perform(get("/api/workspaces"))
                .andExpect(status().isForbidden());
    }

    @Test
    void detail_returnsSeedWorkspace() throws Exception {
        mvc.perform(get("/api/workspaces/" + SEED_WS).header("Authorization", bearer("elshod")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Test Workspace"))
                .andExpect(jsonPath("$.data.ownerId").exists());
    }

    @Test
    void detail_unknownId_returns404() throws Exception {
        mvc.perform(get("/api/workspaces/does-not-exist").header("Authorization", bearer("elshod")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void crud_createUpdateDelete() throws Exception {
        String auth = bearer("elshod");
        String title = uniqueTitle();

        MvcResult created = mvc.perform(post("/api/workspaces")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"bgColor\":\"#abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(title))
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        mvc.perform(put("/api/workspaces/" + id)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "-UPD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(title + "-UPD"));

        mvc.perform(delete("/api/workspaces/" + id).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mvc.perform(get("/api/workspaces/" + id).header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_blankTitle_returns400() throws Exception {
        mvc.perform(post("/api/workspaces")
                        .header("Authorization", bearer("elshod"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
