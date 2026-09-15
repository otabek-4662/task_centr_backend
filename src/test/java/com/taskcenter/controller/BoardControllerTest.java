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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BoardControllerTest {

    private static final String SEED_WS = "6a45163133ff7819b28ef909";
    private static final String SEED_COL_TODO = "6a45163133ff7819b28ef90d";
    private static final String SEED_COL_PROGRESS = "6a45163133ff7819b28ef90e";

    @Autowired
    private MockMvc mvc;

    private String bearer() throws Exception {
        return bearer("elshod");
    }

    private String bearer(String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private String createWorkspace(String auth) throws Exception {
        String title = "BWS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        MvcResult result = mvc.perform(post("/api/workspaces")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    }

    private String createColumn(String auth, String wsId, String body) throws Exception {
        MvcResult result = mvc.perform(post("/api/workspaces/" + wsId + "/columns")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    }

    @Test
    void columns_autoNumberedOnCreate() throws Exception {
        String auth = bearer();
        String wsId = createWorkspace(auth);

        MvcResult r1 = mvc.perform(post("/api/workspaces/" + wsId + "/columns")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"A\"}"))
                .andExpect(status().isOk()).andReturn();
        MvcResult r2 = mvc.perform(post("/api/workspaces/" + wsId + "/columns")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"B\",\"order\":0}"))
                .andExpect(status().isOk()).andReturn();
        MvcResult r3 = mvc.perform(post("/api/workspaces/" + wsId + "/columns")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"C\",\"order\":10}"))
                .andExpect(status().isOk()).andReturn();

        assert JsonPath.<Integer>read(r1.getResponse().getContentAsString(), "$.data.order") == 1;
        assert JsonPath.<Integer>read(r2.getResponse().getContentAsString(), "$.data.order") == 2;
        assert JsonPath.<Integer>read(r3.getResponse().getContentAsString(), "$.data.order") == 10;
    }

    @Test
    void columns_listSortedByOrder() throws Exception {
        mvc.perform(get("/api/workspaces/" + SEED_WS + "/columns")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].order").exists())
                .andExpect(jsonPath("$.data[0].workspaceId").value(SEED_WS));
    }

    @Test
    void column_blankTitle_returns400() throws Exception {
        String auth = bearer();
        String wsId = createWorkspace(auth);

        mvc.perform(post("/api/workspaces/" + wsId + "/columns")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void column_putWithoutTitle_doesNotNullIt() throws Exception {
        String auth = bearer();
        String wsId = createWorkspace(auth);
        String colId = createColumn(auth, wsId, "{\"title\":\"KeepMe\"}");

        mvc.perform(put("/api/workspaces/" + wsId + "/columns/" + colId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("KeepMe"))
                .andExpect(jsonPath("$.data.order").value(5));
    }

    @Test
    void column_patchSingleOrder_persists() throws Exception {
        String auth = bearer();
        String wsId = createWorkspace(auth);
        String colId = createColumn(auth, wsId, "{\"title\":\"P\"}");

        mvc.perform(patch("/api/workspaces/" + wsId + "/columns/" + colId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order").value(42));

        mvc.perform(get("/api/workspaces/" + wsId + "/columns")
                        .header("Authorization", auth))
                .andExpect(jsonPath("$.data[?(@.id=='" + colId + "')].order").value(42));
    }

    @Test
    void columns_patchBatch_reordersAndPersists() throws Exception {
        String auth = bearer();
        String wsId = createWorkspace(auth);
        String a = createColumn(auth, wsId, "{\"title\":\"A\"}");
        String b = createColumn(auth, wsId, "{\"title\":\"B\"}");

        mvc.perform(patch("/api/workspaces/" + wsId + "/columns")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":\"" + a + "\",\"order\":2},{\"id\":\"" + b + "\",\"order\":1}]"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/workspaces/" + wsId + "/columns")
                        .header("Authorization", auth))
                .andExpect(jsonPath("$.data[0].id").value(b))
                .andExpect(jsonPath("$.data[1].id").value(a));
    }

    @Test
    void column_patchWrongWorkspace_returns404() throws Exception {
        String auth = bearer();
        String ws1 = createWorkspace(auth);
        String ws2 = createWorkspace(auth);
        String colId = createColumn(auth, ws1, "{\"title\":\"X\"}");

        mvc.perform(patch("/api/workspaces/" + ws2 + "/columns/" + colId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void column_unknownId_returns404() throws Exception {
        mvc.perform(patch("/api/workspaces/" + SEED_WS + "/columns/nope")
                        .header("Authorization", bearer("xusan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void tasks_createMoveDelete() throws Exception {
        String auth = bearer();
        String wsId = createWorkspace(auth);
        String colA = createColumn(auth, wsId, "{\"title\":\"A\"}");
        String colB = createColumn(auth, wsId, "{\"title\":\"B\"}");

        MvcResult created = mvc.perform(post("/api/workspaces/" + wsId + "/tasks")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"columnId\":\"" + colA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicId").exists())
                .andExpect(jsonPath("$.data.order").value(1))
                .andReturn();
        String taskId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        mvc.perform(patch("/api/workspaces/" + wsId + "/tasks/" + taskId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnId\":\"" + colB + "\",\"order\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.columnId").value(colB))
                .andExpect(jsonPath("$.data.order").value(3));

        mvc.perform(delete("/api/workspaces/" + wsId + "/tasks/" + taskId)
                        .header("Authorization", auth))
                .andExpect(status().isOk());

        mvc.perform(get("/api/workspaces/" + wsId + "/tasks/" + taskId)
                        .header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void board_returnsColumnsWithCards() throws Exception {
        mvc.perform(get("/api/workspaces/" + SEED_WS + "/board")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].cards").exists());
    }

    @Test
    void labels_crud() throws Exception {
        String auth = bearer("xusan");

        MvcResult created = mvc.perform(post("/api/workspaces/" + SEED_WS + "/labels")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"lbl-" + UUID.randomUUID().toString().substring(0, 6) + "\",\"color\":\"RED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.color").value("RED"))
                .andReturn();
        String labelId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        mvc.perform(delete("/api/workspaces/" + SEED_WS + "/labels/" + labelId)
                        .header("Authorization", auth))
                .andExpect(status().isOk());
    }

    @Test
    void members_listsSeedMembers() throws Exception {
        mvc.perform(get("/api/workspaces/" + SEED_WS + "/members")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name=='elshod')]").isArray())
                .andExpect(jsonPath("$.data[?(@.name=='xusan')]").isArray());
    }
}
