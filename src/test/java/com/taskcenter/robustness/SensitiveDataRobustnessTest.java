package com.taskcenter.robustness;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SensitiveDataRobustnessTest {

    @Autowired
    private MockMvc mvc;

    private String createTestUser(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + username + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.token");
    }

    private String createWorkspace(String token, String title) throws Exception {
        String body = mvc.perform(post("/api/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"bgColor\":\"#000000\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
    }

    private String getFirstColumn(String token, String wsId) throws Exception {
        String body = mvc.perform(get("/api/workspaces/" + wsId + "/columns")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data[0].id");
    }

    private String createTask(String token, String wsId, String colId, String title) throws Exception {
        String body = mvc.perform(post("/api/workspaces/" + wsId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnId\":\"" + colId + "\",\"title\":\"" + title + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
    }

    @Test
    void otherUsersDoNotLeakSensitiveData_inWorkspaceMembers() throws Exception {
        String tokenA = createTestUser("sens_user_a");
        String tokenB = createTestUser("sens_user_b");

        String wsId = createWorkspace(tokenA, "Sensitive WS");
        mvc.perform(post("/api/workspaces/" + wsId + "/members")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"sens_user_b\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated());

        // user_a lists users in workspace
        mvc.perform(get("/api/users?workspaceId=" + wsId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.name=='sens_user_b')].password").doesNotExist())
                .andExpect(jsonPath("$.data.content[?(@.name=='sens_user_b')].telegramLinkToken").doesNotExist())
                .andExpect(jsonPath("$.data.content[?(@.name=='sens_user_b')].telegramChatId").doesNotExist());
    }

    @Test
    void otherUsersDoNotLeakSensitiveData_inTaskAssignees() throws Exception {
        String tokenA = createTestUser("sens_user_c");
        String tokenB = createTestUser("sens_user_d");

        String wsId = createWorkspace(tokenA, "Sensitive WS 2");
        mvc.perform(post("/api/workspaces/" + wsId + "/members")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"sens_user_d\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated());

        String colId = getFirstColumn(tokenA, wsId);
        String taskId = createTask(tokenA, wsId, colId, "Secret Task");

        // Assign user_d
        String bJson = mvc.perform(get("/api/me").header("Authorization", "Bearer " + tokenB))
                .andReturn().getResponse().getContentAsString();
        String userIdB = JsonPath.read(bJson, "$.data.id");

        mvc.perform(post("/api/workspaces/" + wsId + "/tasks/" + taskId + "/assign?userId=" + userIdB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // user_a views task
        mvc.perform(get("/api/tasks/" + taskId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignees[?(@.name=='sens_user_d')].password").doesNotExist())
                .andExpect(jsonPath("$.data.assignees[?(@.name=='sens_user_d')].telegramLinkToken").doesNotExist())
                .andExpect(jsonPath("$.data.assignees[?(@.name=='sens_user_d')].telegramChatId").doesNotExist());
    }
}
